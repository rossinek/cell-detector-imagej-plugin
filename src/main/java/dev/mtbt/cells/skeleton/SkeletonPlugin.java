package dev.mtbt.cells.skeleton;

import ij.ImageListener;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;

import java.awt.Point;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import dev.mtbt.HyperstackHelper;
import dev.mtbt.ImageJUtils;
import dev.mtbt.Utils;
import dev.mtbt.cells.CellCollection;
import dev.mtbt.cells.CellFrame;
import dev.mtbt.cells.PolylineCellFrame;
import dev.mtbt.graph.Vertex;
import dev.mtbt.gui.DialogActions;
import dev.mtbt.gui.ExpandablePanel;
import dev.mtbt.gui.RunnableCheckBox;
import dev.mtbt.gui.RunnableSpinner;
import dev.mtbt.gui.StackWindowWithPanel;
import dev.mtbt.util.Pair;
import dev.mtbt.vendor.shapeindex.ShapeIndexMap;

public abstract class SkeletonPlugin extends DynamicCommand implements ImageListener {

  @Parameter
  protected ImagePlus imp;

  @Parameter
  protected UIService uiService;

  private ImagePlus impIndexMap = null;

  protected ImagePlus impPreviewStack = null;
  protected Skeleton skeleton = null;

  protected StackWindowWithPanel dialog;
  protected DialogActions dialogActions;
  protected JPanel dialogContent;
  protected RunnableSpinner blurRadiusSlider;
  protected RunnableSpinner thresholdSlider;
  protected RunnableCheckBox shapeIndexCheckBox;
  protected RunnableCheckBox skeletonCheckBox;

  protected CellCollection cellCollection = null;

  boolean initialized = false;

  @Override
  public void run() {
    this.initComponents();
    this.preview();
  }

  protected boolean initComponents() {
    if (this.initialized || this.imp == null) {
      return false;
    }
    ImagePlus.addImageListener(this);
    this.impPreviewStack = this.imp.duplicate();

    this.dialogContent = new JPanel();
    this.dialogContent.setLayout(new BoxLayout(this.dialogContent, BoxLayout.Y_AXIS));

    JPanel advancedPanel = new JPanel();
    advancedPanel.setLayout(new BoxLayout(advancedPanel, BoxLayout.Y_AXIS));

    this.blurRadiusSlider = new RunnableSpinner(4.0, 0.0, 10.0, 0.2, this::preview);
    addCenteredComponent(advancedPanel, new JLabel("Blur"));
    addCenteredComponent(advancedPanel, this.blurRadiusSlider);
    this.thresholdSlider = new RunnableSpinner(0.0, -1.0, 1.0, 0.1, this::preview);
    addCenteredComponent(advancedPanel, new JLabel("Threshold"));
    addCenteredComponent(advancedPanel, this.thresholdSlider);
    this.shapeIndexCheckBox = new RunnableCheckBox("show shape index map", this::preview);
    addCenteredComponent(advancedPanel, this.shapeIndexCheckBox);
    this.skeletonCheckBox = new RunnableCheckBox("show skeleton", this::preview);
    addCenteredComponent(advancedPanel, this.skeletonCheckBox);

    this.dialogContent.add(Box.createVerticalStrut(20));
    ExpandablePanel expandablePanel =
        new ExpandablePanel("advanced settings >>", advancedPanel, () -> this.dialog.pack());
    addCenteredComponent(this.dialogContent, expandablePanel);

    this.dialogActions = new DialogActions(this::done, this::cleanup);

    this.dialog = new StackWindowWithPanel(this.impPreviewStack);
    this.dialog.getSidePanel().add(this.dialogContent, BorderLayout.NORTH);
    this.dialog.getSidePanel().add(this.dialogActions, BorderLayout.SOUTH);
    this.dialog.pack();

    this.initialized = true;
    return true;
  }

  protected void addCenteredComponent(JPanel panel, JComponent component) {
    component.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(component);
  }

  protected ImagePlus getImpIndexMap() {
    if (this.impIndexMap == null) {
      computeShapeIndexMap();
      thresholdShapeIndexMap();
    }
    return this.impIndexMap;
  }

  protected Skeleton getSkeleton() {
    if (this.skeleton == null) {
      this.skeleton = new Skeleton(this.getImpIndexMap().duplicate());
    }
    return this.skeleton;
  }

  protected ImagePlus getOriginalFrame() {
    return HyperstackHelper.extractFrame(imp, this.impPreviewStack.getChannel(),
        this.impPreviewStack.getSlice(), this.impPreviewStack.getT());
  }

  public void preview() {
    if (this.initialized) {
      this.impIndexMap = null;
      this.skeleton = null;
      // if (this.skeletonCheckBox.isSelected()) {
      // this.impPreview.setProcessor(this.getSkeleton().toImagePlus().getProcessor());
      // } else if (this.shapeIndexCheckBox.isSelected()) {
      // this.impPreview.setProcessor(getImpIndexMap().getProcessor());
      // } else {
      // this.impPreview.setProcessor(impOriginalFrame.getProcessor());
      // }
      // this.impPreview.show();
      // this.impPreviewStack.setC((int) channelSlider.getValue());
      // this.impPreviewStack.setT((int) frameSlider.getValue());
      this.impPreviewStack.show();
      this.updateAndDrawCells();
    }
  }

  protected void done() {
    cleanup();
  }

  protected void cleanup() {
    impPreviewStack.close();
  }

  private void computeShapeIndexMap() {
    ImagePlus indexMap = ShapeIndexMap.getShapeIndexMap(this.getOriginalFrame(),
        (double) blurRadiusSlider.getValue());
    if (this.impIndexMap == null) {
      this.impIndexMap = indexMap;
    } else {
      this.impIndexMap.setProcessor(indexMap.getProcessor());
    }
  }

  private void thresholdShapeIndexMap() {
    FloatProcessor fp = (FloatProcessor) impIndexMap.getProcessor();
    int length = impIndexMap.getProcessor().getPixelCount();
    for (int i = 0; i < length; i++) {
      float val = fp.getf(i);
      fp.setf(i, val > (double) thresholdSlider.getValue() ? val : Float.NEGATIVE_INFINITY);
    }
  }

  protected Spine performSearch(Point point) {
    Spine spine = this.getSkeleton().findSpine(point);
    return spine;
  }

  protected List<Spine> performSearch(List<Point> points) {
    if (points.isEmpty()) {
      return new ArrayList<Spine>();
    }

    ArrayList<Pair<Point, Spine>> spines =
        points.stream().map(point -> new Pair<>(point, this.performSearch(point)))
            .collect(Collectors.toCollection(ArrayList::new));

    return this.fixConflicts(spines).stream().map(pair -> pair.getValue())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  protected List<Pair<Point, Spine>> fixConflicts(List<Pair<Point, Spine>> spines) {
    boolean change;
    do {
      change = false;
      ListIterator<Pair<Point, Spine>> iterator = spines.listIterator();
      while (iterator.hasNext()) {
        Pair<Point, Spine> spine = iterator.next();
        for (int i = iterator.nextIndex(); i < spines.size(); i++) {
          if (spine.getValue().overlaps(spines.get(i).getValue())) {
            dev.mtbt.graph.Point p1 = new dev.mtbt.graph.Point(spine.getKey());
            dev.mtbt.graph.Point p2 = new dev.mtbt.graph.Point(spines.get(i).getKey());
            Spine.splitOverlap(new Pair<>(p1, spine.getValue()),
                new Pair<>(p2, spines.get(i).getValue()),
                p -> this.getImpIndexMap().getProcessor().getf(p.x, p.y));
            change = true;
            break;
          }
        }
      }
    } while (change);

    return spines;
  }

  protected CellFrame spineToCellFrame(Spine spine) {
    List<Point2D> polyline = spine.toPolyline();
    if (polyline.size() >= 2) {
      Vertex sv1 = spine.getE1().getSkeletonVertex();
      Vertex sv2 = spine.getE2().getSkeletonVertex();
      if (sv1 != null && sv1.isLeaf()) {
        polyline.set(0, tryExtendLineToBlobEnd(polyline.get(1), polyline.get(0)));
      }
      if (sv2 != null && sv2.isLeaf()) {
        int last = polyline.size() - 1;
        polyline.set(last, tryExtendLineToBlobEnd(polyline.get(last - 1), polyline.get(last)));
      }
    }
    return new PolylineCellFrame(polyline);
  }

  private Point2D tryExtendLineToBlobEnd(Point2D lineBegin, Point2D lineEnd) {
    int searchRadius = 20;
    // vector
    double vx = lineEnd.getX() - lineBegin.getX();
    double vy = lineEnd.getY() - lineBegin.getY();
    double length = Math.sqrt((vx * vx + vy * vy));
    vx = vx / length;
    vy = vy / length;

    Point2D searchEnd =
        new Point2D.Double(lineEnd.getX() + vx * searchRadius, lineEnd.getY() + vy * searchRadius);
    List<Point> line = Utils.rasterizeLine(lineEnd, searchEnd);
    double threshold = (double) thresholdSlider.getValue();
    for (int index = 0; index < line.size(); index++) {
      Point point = line.get(index);
      if (this.getImpIndexMap().getProcessor().getf(point.x, point.y) <= threshold) {
        return index > 0 ? line.get(index - 1) : lineEnd;
      }
    }
    return lineEnd;
  }

  protected void updateAndDrawCells() {
    if (this.cellCollection != null) {
      RoiManager roiManager = ImageJUtils.getRoiManager();
      roiManager.reset();
      this.cellCollection.getCells(this.impPreviewStack.getT()).stream()
          .map(cell -> cell.getObservedRoi(this.impPreviewStack.getT())).filter(roi -> roi != null)
          .forEach(roi -> roiManager.addRoi(roi));
      roiManager.runCommand("show all");
    }
  }

  @Override
  public void imageOpened(ImagePlus imp) {
    // Ignore
  }

  @Override
  public void imageClosed(ImagePlus imp) {
    // Ignore
  }

  @Override
  public void imageUpdated(ImagePlus image) {
    if (image == this.impPreviewStack) {
      this.preview();
    }
  }
}
