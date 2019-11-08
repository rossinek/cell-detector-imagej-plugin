package dev.mtbt.cells.skeleton;

import dev.mtbt.ImageJUtils;
import dev.mtbt.util.Pair;
import dev.mtbt.cells.Cell;
import dev.mtbt.cells.ICellDetector;
import dev.mtbt.cells.ICellFrame;
import dev.mtbt.cells.skeleton.Skeleton;
import dev.mtbt.cells.skeleton.Spine;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Toolbar;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.swing.JDialog;

import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.tool.ToolService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;
import dev.mtbt.HyperstackHelper;
import dev.mtbt.ShapeIndexMap;

@Plugin(type = Command.class, menuPath = "Developement>Skeleton>Cell Detector")
public class CellDetector extends InteractiveCommand implements Initializable, ICellDetector<Spine> {

  @Parameter
  private ImagePlus imp;

  @Parameter
  private ToolService toolService;

  @Parameter
  private UIService uiService;

  // Dialog inputs
  @Parameter(persist = false, label = "Channel", style = NumberWidget.SCROLL_BAR_STYLE, min = "1")
  private int channelInput;

  @Parameter(persist = false, label = "Frame", style = NumberWidget.SCROLL_BAR_STYLE, min = "1")
  private int frameInput;

  @Parameter(persist = false, label = "Shape index map gaussian blur radius", style = NumberWidget.SCROLL_BAR_STYLE, min = "0", max = "10", stepSize = "0.1")
  private double blurRadiusInput;

  @Parameter(persist = false, label = "Shape index map threshold", style = NumberWidget.SCROLL_BAR_STYLE, min = "-1", max = "1", stepSize = "0.1")
  private double thresholdInput;

  @Parameter(label = "Select cells", callback = "onSelectCellsClick")
  private Button selectCellsButton;

  @Parameter(label = "Clear points", callback = "onClearPointsClick")
  private Button clearPointsButton;

  @Parameter(label = "Clear selected cells", callback = "onClearSelectedCellsClick")
  private Button clearSelectedCellsButton;

  @Parameter(label = "run", callback = "onRunClick")
  private Button runButton;

  @Parameter(label = "done", callback = "onDoneClick")
  private Button doneButton;

  @Parameter(type = ItemIO.OUTPUT)
  private List<Cell<ICellFrame>> cells = new ArrayList<>();

  private ImagePlus impIndexMap = null;

  private ArrayList<Point> initialPoints = new ArrayList<>();

  CompletableFuture<List<Cell<ICellFrame>>> result = new CompletableFuture<>();

  @Override
  public Future<List<Cell<ICellFrame>>> output() {
    return this.result;
  }

  @Override
  public void initialize() {
    System.out.println("> initialize");
    if (this.imp == null)
      return;
    final MutableModuleItem<Integer> frameInputItem = getInfo().getMutableInput("frameInput", Integer.class);
    frameInputItem.setMaximumValue(imp.getNFrames());
    final MutableModuleItem<Integer> channelInputItem = getInfo().getMutableInput("channelInput", Integer.class);
    channelInputItem.setMaximumValue(imp.getNChannels());
    this.channelInput = imp.getChannel();
    this.frameInput = imp.getFrame();
    this.blurRadiusInput = 4.0;
    this.thresholdInput = 0.0;
  }

  @Override
  public void run() {
    System.out.println("> run");
    computeShapeIndexMap();
    thresholdShapeIndexMap();
    impIndexMap.show();
  }

  protected void onSelectCellsClick() {
    System.out.println("> onSelectCellsClick");
    if (impIndexMap != null) {
      impIndexMap.deleteRoi();
    }
    Toolbar.getInstance().setTool(Toolbar.POINT);
  }

  protected void onClearPointsClick() {
    System.out.println("> onClearPointsClick");
    if (impIndexMap != null) {
      impIndexMap.deleteRoi();
    }
  }

  protected void onClearSelectedCellsClick() {
    System.out.println("> onClearSelectedCellsClick");
    cells.clear();
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.runCommand("show all");
  }

  protected void onRunClick() {
    PolygonRoi roi = impIndexMap != null ? (PolygonRoi) impIndexMap.getRoi() : null;
    if (roi == null) {
      uiService.showDialog("There are no points selected.");
      return;
    }
    collectSelectedPoints();
    performSearch();
  }

  protected void onDoneClick() {
    result.complete(this.cells);
    impIndexMap.close();
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.close();

    Window[] windows = Window.getWindows();
    for (Window window : windows) {
      if (JDialog.class.isInstance(window)) {
        JDialog dialog = (JDialog) window;
        if (dialog.getTitle().contains(this.getInfo().getTitle())) {
          dialog.dispose();
          return;
        }
      }
    }
    uiService.showDialog("Please close detector window manually");
  }

  private void collectSelectedPoints() {
    initialPoints.clear();
    PolygonRoi roi = impIndexMap != null ? (PolygonRoi) impIndexMap.getRoi() : null;
    if (roi == null) {
      return;
    }
    System.out.println("> collect selected points (" + roi.getFloatPolygon().npoints + ")");
    int[] xPoints = roi.getPolygon().xpoints;
    int[] yPoints = roi.getPolygon().ypoints;
    for (int i = 0; i < xPoints.length; i++) {
      initialPoints.add(new Point(xPoints[i], yPoints[i]));
    }
  }

  private void computeShapeIndexMap() {
    ImagePlus frame = HyperstackHelper.extractFrame(imp, channelInput, imp.getSlice(), frameInput);
    ImagePlus indexMap = ShapeIndexMap.getShapeIndexMap(frame, blurRadiusInput);
    if (impIndexMap == null) {
      impIndexMap = indexMap;
    } else {
      impIndexMap.setProcessor(indexMap.getProcessor());
    }
  }

  private void thresholdShapeIndexMap() {
    FloatProcessor fp = (FloatProcessor) impIndexMap.getProcessor();
    int length = impIndexMap.getProcessor().getPixelCount();
    for (int i = 0; i < length; i++) {
      float val = fp.getf(i);
      fp.setf(i, val > thresholdInput ? val : Float.NEGATIVE_INFINITY);
    }
  }

  private void performSearch() {
    if (initialPoints.isEmpty()) {
      uiService.showDialog("There are no points selected.");
      return;
    }
    System.out.println("> build skeleton...");

    Skeleton skeleton = new Skeleton(impIndexMap);

    System.out.println("> performing search...");

    ArrayList<Pair<Point, Spine>> spines = new ArrayList<>();
    initialPoints.forEach((point) -> spines.add(new Pair<Point, Spine>(point, skeleton.findSpine(point))));

    System.out.println("> splitting concurrent spines");

    boolean change;
    do {
      change = false;
      ListIterator<Pair<Point, Spine>> iterator = spines.listIterator();
      while (iterator.hasNext()) {
        Pair<Point, Spine> spine = iterator.next();
        for (int i = iterator.nextIndex(); i < spines.size(); i++) {
          if (spine.getValue().overlaps(spines.get(i).getValue())) {
            Point p1 = spine.getKey();
            Point p2 = spines.get(i).getKey();
            Spine[] newSpines = spine.getValue().split(new dev.mtbt.graph.Point(p1), new dev.mtbt.graph.Point(p2),
                p -> impIndexMap.getProcessor().getf(p.x, p.y));
            iterator.set(new Pair<>(p1, newSpines[0]));
            spines.set(i, new Pair<>(p2, newSpines[1]));
            change = true;
            break;
          }
        }
      }
    } while (change);

    System.out.println("> done.");

    // cells.clear();
    // spines.forEach((spine) -> cells.add(new Cell(spine.getValue())));
    spines.forEach((spine) -> cells.add(new Cell<>(frameInput, spine.getValue())));
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    cells.forEach(cell -> roiManager.addRoi(cell.toRoi(frameInput)));
    roiManager.runCommand("show all");
  }

  // private void displayCells () {
  // RoiManager roiManager = ImageJUtils.getRoiManager();
  // roiManager.reset();
  // cells.forEach(cell -> roiManager.addRoi(cell.toRoi()));
  // roiManager.runCommand("show all with labels");
  // }
}
