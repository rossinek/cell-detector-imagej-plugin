package dev.mtbt.cells.skeleton;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.awt.Point;
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
import dev.mtbt.Utils;
import dev.mtbt.cells.CellFrame;
import dev.mtbt.cells.PolylineCellFrame;
import dev.mtbt.graph.Vertex;
import dev.mtbt.gui.DialogWindow;
import dev.mtbt.gui.ExpandablePanel;
import dev.mtbt.gui.RunnableCheckBox;
import dev.mtbt.gui.RunnableSpinner;
import dev.mtbt.util.Pair;
import dev.mtbt.vendor.shapeindex.ShapeIndexMap;

public abstract class SkeletonPlugin extends DynamicCommand {

  @Parameter
  protected ImagePlus imp;

  @Parameter
  protected UIService uiService;

  private ImagePlus impOriginalFrame = null;
  private ImagePlus impIndexMap = null;

  protected ImagePlus impPreview = new ImagePlus();
  protected Skeleton skeleton = null;

  protected DialogWindow dialog;
  protected JPanel dialogContent;
  protected RunnableSpinner channelSlider;
  protected RunnableSpinner frameSlider;
  protected RunnableSpinner blurRadiusSlider;
  protected RunnableSpinner thresholdSlider;
  protected RunnableCheckBox originalCheckBox;
  protected RunnableCheckBox skeletonCheckBox;

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

    this.dialogContent = new JPanel();
    dialogContent.setLayout(new BoxLayout(dialogContent, BoxLayout.Y_AXIS));

    this.channelSlider = new RunnableSpinner(imp.getC(), 1, imp.getNChannels(), this::preview);
    addCenteredComponent(dialogContent, new JLabel("Channel"));
    addCenteredComponent(dialogContent, channelSlider);

    this.frameSlider = new RunnableSpinner(imp.getFrame(), 1, imp.getNFrames(), this::preview);
    addCenteredComponent(dialogContent, new JLabel("Frame"));
    addCenteredComponent(dialogContent, frameSlider);

    JPanel advancedPanel = new JPanel();
    advancedPanel.setLayout(new BoxLayout(advancedPanel, BoxLayout.Y_AXIS));

    this.blurRadiusSlider = new RunnableSpinner(4.0, 0.0, 10.0, 0.2, this::preview);
    addCenteredComponent(advancedPanel, new JLabel("Blur"));
    addCenteredComponent(advancedPanel, blurRadiusSlider);
    this.thresholdSlider = new RunnableSpinner(0.0, -1.0, 1.0, 0.1, this::preview);
    addCenteredComponent(advancedPanel, new JLabel("Threshold"));
    addCenteredComponent(advancedPanel, thresholdSlider);
    this.originalCheckBox = new RunnableCheckBox("show original frame", this::preview);
    addCenteredComponent(advancedPanel, originalCheckBox);
    this.skeletonCheckBox = new RunnableCheckBox("show skeleton", this::preview);
    addCenteredComponent(advancedPanel, skeletonCheckBox);

    dialogContent.add(Box.createVerticalStrut(20));
    ExpandablePanel expandablePanel =
        new ExpandablePanel("advanced settings >>", advancedPanel, () -> this.dialog.pack());
    addCenteredComponent(dialogContent, expandablePanel);

    this.dialog = new DialogWindow("Skeleton plugin", this::done, this::cleanup);
    this.dialog.setContent(dialogContent);
    this.dialog.setVisible(true);

    initialized = true;
    return true;
  }

  protected void addCenteredComponent(JPanel panel, JComponent component) {
    component.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(component);
  }

  public void preview() {
    if (this.initialized) {
      computeShapeIndexMap();
      thresholdShapeIndexMap();
      if (this.originalCheckBox.isSelected()) {
        impPreview.setProcessor(impOriginalFrame.getProcessor());
      } else if (this.skeletonCheckBox.isSelected()) {
        this.skeleton = new Skeleton(this.impIndexMap.duplicate());
        impPreview.setProcessor(this.skeleton.toImagePlus().getProcessor());
      } else {
        impPreview.setProcessor(impIndexMap.getProcessor());
      }
      impPreview.show();
    }
  }

  protected void done() {
    cleanup();
    this.dialog.setVisible(false);
  }

  protected void cleanup() {
    impPreview.close();
    this.dialog.setVisible(false);
  }

  private void computeShapeIndexMap() {
    this.skeleton = null;
    this.impOriginalFrame = HyperstackHelper.extractFrame(imp, (int) channelSlider.getValue(),
        imp.getSlice(), (int) frameSlider.getValue());
    ImagePlus indexMap =
        ShapeIndexMap.getShapeIndexMap(this.impOriginalFrame, (double) blurRadiusSlider.getValue());
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
      fp.setf(i, val > (double) thresholdSlider.getValue() ? val : Float.NEGATIVE_INFINITY);
    }
  }

  protected Spine performSearch(Point point) {
    if (this.skeleton == null) {
      this.skeleton = new Skeleton(this.impIndexMap.duplicate());
    }
    Spine spine = this.skeleton.findSpine(point);
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
                p -> this.impIndexMap.getProcessor().getf(p.x, p.y));
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
      if (this.impIndexMap.getProcessor().getf(point.x, point.y) <= threshold) {
        return index > 0 ? line.get(index - 1) : lineEnd;
      }
    }
    return lineEnd;
  }
}
