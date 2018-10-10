package dev.mtbt.cells;

import dev.mtbt.Utils;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.filter.EDM;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import net.imagej.ops.OpService;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
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
  @Parameter(persist = false, label = "Channel", callback = "channelInputChange")
  private int channelInput = 1;
  @Parameter(persist = false, label = "Shape index map gaussian blur radius", style = NumberWidget.SCROLL_BAR_STYLE, min = "0", max = "10", stepSize = "0.1")
  private double blurRadiusInput = 4.0;
  @Parameter(persist = false, label = "Shape index map threshold", style = NumberWidget.SCROLL_BAR_STYLE, min = "-1", max = "1", stepSize = "0.1")
  private double thresholdInput = 0.0;

  @Parameter(label = "Select cells", callback = "selectCellsButtonClick")
  private Button selectCellsButton;

  @Parameter(label = "Run!", callback = "runButtonClick")
  private Button doneButton;

  private ImagePlus impIndexMap = null;
  private ArrayList<Point> initialPoints = new ArrayList<>();
  private ArrayList<Cell> cells = new ArrayList<>();

  @Override
  public void initialize() {
    System.out.println("> initialize");
    if (imp != null) {
      channelInput = imp.getChannel();
    }
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

  protected void channelInputChange () {
    System.out.println("> channelInputChange");
    channelInput = Math.max(1, Math.min(channelInput, imp.getNChannels()));
  }

  protected void selectCellsButtonClick () {
    System.out.println("> selectCellsButtonClick");
    if (impIndexMap != null) {
      impIndexMap.deleteRoi();
      impIndexMap.show();
    }
    Toolbar.getInstance().setTool(Toolbar.POINT);

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
    System.out.println("Collect selected points (" + roi.getFloatPolygon().npoints + ")");
    int[] xPoints = roi.getPolygon().xpoints;
    int[] yPoints = roi.getPolygon().ypoints;
    for (int i = 0; i < xPoints.length; i++) {
      initialPoints.add(new Point(xPoints[i], yPoints[i]));
    }
  }

  private void computeShapeIndexMap () {
    ImagePlus frame = HyperstackHelper.extractFrame(imp, channelInput, imp.getSlice(), imp.getFrame());
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
    skeleton.toImagePlus().show();
    //    skeleton.segmentation();
    //    segmentSkeleton(skeleton).forEach(polyLine -> cells.add(new Cell(polyLine)));
  }


  private void displayCells () {
    RoiManager roiManager = Utils.getRoiManager();
    roiManager.reset();
    cells.forEach(cell -> roiManager.addRoi(cell.toRoi()));
    roiManager.runCommand("show all with labels");
  }
}