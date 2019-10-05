package dev.mtbt.cells;

import dev.mtbt.ImageJUtils;
import dev.mtbt.cells.skeleton.Skeleton;
import dev.mtbt.cells.skeleton.Spine;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Toolbar;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import javafx.util.Pair;
import net.imagej.ops.OpService;
import org.scijava.Initializable;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.ListIterator;

@SuppressWarnings("restriction")
@Plugin(type = Command.class, menuPath = "Developement>Cell detector")
public class CellDetector extends InteractiveCommand implements Initializable {
  @Parameter
  private ImagePlus imp;
  @Parameter
  private ToolService toolService;
  @Parameter
  private UIService uiService;
  @Parameter
  OpService ops;

  // Dialog inputs
  @Parameter(persist = false, label = "Channel", style = NumberWidget.SCROLL_BAR_STYLE, min = "1")
  private int channelInput;
  @Parameter(persist = false, label = "Frame", style = NumberWidget.SCROLL_BAR_STYLE, min = "1")
  private int frameInput;
  @Parameter(persist = false, label = "Shape index map gaussian blur radius", style = NumberWidget.SCROLL_BAR_STYLE, min = "0", max = "10", stepSize = "0.1")
  private double blurRadiusInput;
  @Parameter(persist = false, label = "Shape index map threshold", style = NumberWidget.SCROLL_BAR_STYLE, min = "-1", max = "1", stepSize = "0.1")
  private double thresholdInput;

  @Parameter(label = "Select cells", callback = "selectCellsButtonClick")
  private Button selectCellsButton;

  @Parameter(label = "Clear points", callback = "clearPointsButtonClick")
  private Button clearPointsButton;

  @Parameter(label = "Clear selected cells", callback = "clearSelectedCellsButtonClick")
  private Button clearSelectedCellsButton;

  @Parameter(label = "Run!", callback = "runButtonClick")
  private Button doneButton;

  private ImagePlus impIndexMap = null;
  private ArrayList<Point> initialPoints = new ArrayList<>();
  private ArrayList<Cell> cells = new ArrayList<>();

  @Override
  public void initialize () {
    System.out.println("> initialize");
    if (imp == null) return;
    final MutableModuleItem<Integer> frameInputItem = getInfo().getMutableInput("frameInput", int.class);
    frameInputItem.setMaximumValue(imp.getNFrames());
    final MutableModuleItem<Integer> channelInputItem = getInfo().getMutableInput("channelInput", int.class);
    channelInputItem.setMaximumValue(imp.getNChannels());
    channelInput = imp.getChannel();
    frameInput = imp.getFrame();
    blurRadiusInput = 4.0;
    thresholdInput = 0.0;
  }

  @Override
  public void run () {
    System.out.println("> run");
    computeShapeIndexMap();
    thresholdShapeIndexMap();
    impIndexMap.show();
  }

  @Override
  public void preview () {
    System.out.println("> preview");
    run();
  }

  protected void selectCellsButtonClick () {
    System.out.println("> selectCellsButtonClick");
    if (impIndexMap != null) {
      impIndexMap.deleteRoi();
    }
    Toolbar.getInstance().setTool(Toolbar.POINT);
  }

  protected void clearPointsButtonClick () {
    System.out.println("> clearPointsButtonClick");
    if (impIndexMap != null) {
      impIndexMap.deleteRoi();
    }
  }

  protected void clearSelectedCellsButtonClick () {
    System.out.println("> clearSelectedCellsButtonClick");
    cells.clear();
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.runCommand("show all");
  }

  protected void runButtonClick () {
    PolygonRoi roi = impIndexMap != null ? (PolygonRoi) impIndexMap.getRoi() : null;
    if (roi == null) {
      uiService.showDialog("There are no points selected.");
      return;
    }
    collectSelectedPoints();
    performSearch();
  }

  private void collectSelectedPoints () {
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

  private void computeShapeIndexMap () {
    ImagePlus frame = HyperstackHelper.extractFrame(imp, channelInput, imp.getSlice(), frameInput);
    ImagePlus indexMap = ShapeIndexMap.getShapeIndexMap(frame, blurRadiusInput);
    if (impIndexMap == null) {
      impIndexMap = indexMap;
    } else {
      impIndexMap.setProcessor(indexMap.getProcessor());
    }
  }

  private void thresholdShapeIndexMap () {
    FloatProcessor fp = (FloatProcessor) impIndexMap.getProcessor();
    int length = impIndexMap.getProcessor().getPixelCount();
    for (int i = 0; i < length; i++) {
      float val = fp.getf(i);
      fp.setf(i, val > thresholdInput ? val : Float.NEGATIVE_INFINITY);
    }
  }

  private void performSearch () {
    if (initialPoints.isEmpty()) {
      uiService.showDialog("There are no points selected.");
      return;
    }
    Skeleton skeleton = new Skeleton(impIndexMap);

    ArrayList<Pair<Point, Spine>> spines = new ArrayList<>();
    initialPoints.forEach((point) -> spines.add(new Pair<Point, Spine>(point, skeleton.findSpine(point))));
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
            Spine[] newSpines = spine.getValue()
                    .split(new dev.mtbt.graph.Point(p1), new dev.mtbt.graph.Point(p2), p -> impIndexMap.getProcessor().getf(p.x, p.y));
            iterator.set(new Pair<>(p1, newSpines[0]));
            spines.set(i, new Pair<>(p2, newSpines[1]));
            change = true;
            break;
          }
        }
      }
    } while (change);

    // cells.clear();
    spines.forEach((spine) -> cells.add(new Cell(spine.getValue())));
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    cells.forEach(cell -> roiManager.addRoi(cell.toRoi()));
    roiManager.runCommand("show all");
  }


  // private void displayCells () {
  //   RoiManager roiManager = ImageJUtils.getRoiManager();
  //   roiManager.reset();
  //   cells.forEach(cell -> roiManager.addRoi(cell.toRoi()));
  //   roiManager.runCommand("show all with labels");
  // }
}
