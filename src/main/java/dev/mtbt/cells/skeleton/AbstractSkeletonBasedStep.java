package dev.mtbt.cells.skeleton;

import ij.ImagePlus;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.process.FloatProcessor;

import java.awt.Point;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import dev.mtbt.imagej.HyperstackHelper;
import dev.mtbt.util.Geometry;
import dev.mtbt.cells.CellCollection;
import dev.mtbt.cells.AbstractCellFrame;
import dev.mtbt.cells.ICellsPluginStep;
import dev.mtbt.graph.Vertex;
import dev.mtbt.gui.ExpandablePanel;
import dev.mtbt.gui.RunnableCheckBox;
import dev.mtbt.gui.RunnableSpinner;
import dev.mtbt.util.Pair;
import dev.mtbt.vendor.shapeindex.ShapeIndexMap;

public abstract class AbstractSkeletonBasedStep implements ICellsPluginStep {
  static private SkeletonPluginCache cache;

  private boolean initialized = false;
  protected ImagePlus imp;
  protected CellCollection cellCollection;
  protected JPanel dialogContent;

  protected RunnableSpinner blurRadiusSlider;
  protected RunnableSpinner thresholdSlider;
  protected RunnableCheckBox shapeIndexCheckBox;
  protected RunnableCheckBox skeletonCheckBox;

  protected AbstractSkeletonBasedStep() {
  }

  @Override
  public JComponent init(ImagePlus imp, CellCollection collection) {
    if (this.initialized) {
      throw new IllegalAccessError("Already initialized");
    }
    this.imp = imp;
    this.cellCollection = collection;

    if (AbstractSkeletonBasedStep.cache == null
        || !AbstractSkeletonBasedStep.cache.isTarget(this.imp)) {
      AbstractSkeletonBasedStep.cache = new SkeletonPluginCache(this.imp);
    } else {
      this.imp.setSlice(AbstractSkeletonBasedStep.cache.slice);
      this.imp.setC(AbstractSkeletonBasedStep.cache.channel);
      this.imp.setT(AbstractSkeletonBasedStep.cache.frame);
    }

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

    ExpandablePanel expandablePanel =
        new ExpandablePanel("advanced settings >>", advancedPanel, () -> {
          // this.dialog.pack()
        });
    addCenteredComponent(this.dialogContent, expandablePanel);
    this.initialized = true;
    return this.dialogContent;
  }

  protected void addCenteredComponent(JPanel panel, JComponent component) {
    component.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(component);
  }

  private String getIndexMapId() {
    int slice = this.imp.getSlice();
    int channel = this.imp.getChannel();
    int frame = this.imp.getT();
    double blur = (double) blurRadiusSlider.getValue();
    return slice + ";" + channel + ";" + frame + ";" + blur;
  }

  private String getSkeletonId() {
    double threshold = (double) thresholdSlider.getValue();
    return this.getIndexMapId() + ";" + threshold;
  }

  protected Skeleton getSkeleton() {
    String sId = this.getSkeletonId();
    Skeleton skeleton = AbstractSkeletonBasedStep.cache.getSkeleton(sId);
    if (skeleton == null) {
      skeleton = new Skeleton(this.getShapeIndexMap().duplicate());
      AbstractSkeletonBasedStep.cache.setSkeleton(sId, skeleton);
    }
    return skeleton;
  }

  private ImagePlus getShapeIndexMap() {
    String simId = this.getIndexMapId();
    double blur = (double) blurRadiusSlider.getValue();
    ImagePlus indexMap = AbstractSkeletonBasedStep.cache.getIndexMap(simId);
    if (indexMap == null) {
      indexMap = ShapeIndexMap.getShapeIndexMap(this.getOriginalFrame(), blur);
      AbstractSkeletonBasedStep.cache.setIndexMap(simId, indexMap);
    }
    return thresholdShapeIndexMap(indexMap);
  }

  private ImagePlus thresholdShapeIndexMap(ImagePlus input) {
    ImagePlus impIndexMap = input.duplicate();
    FloatProcessor fp = (FloatProcessor) impIndexMap.getProcessor();
    int length = impIndexMap.getProcessor().getPixelCount();
    for (int i = 0; i < length; i++) {
      float val = fp.getf(i);
      fp.setf(i, val > (double) thresholdSlider.getValue() ? val : Float.NEGATIVE_INFINITY);
    }
    return impIndexMap;
  }

  protected ImagePlus getOriginalFrame() {
    return HyperstackHelper.extractFrame(imp, this.imp.getChannel(), this.imp.getSlice(),
        this.imp.getT());
  }

  private void showImageOverlay(ImagePlus overlay) {
    ImageRoi roi = new ImageRoi(0, 0, overlay.getProcessor());
    roi.setName("__image-overlay__");
    Overlay overlayList = this.imp.getOverlay();
    if (overlayList == null) {
      overlayList = new Overlay();
    }
    overlayList.add(roi);
    this.imp.setOverlay(overlayList);
  }

  private void removeImageOverlay() {
    Overlay overlayList = this.imp.getOverlay();
    if (overlayList != null) {
      overlayList.remove("__image-overlay__");
      this.imp.setOverlay(overlayList);
    }
  }

  public void preview() {
    this.imp.updateAndDraw();
  }

  @Override
  public void cleanup() {
    if (this.imp != null) {
      imp.setOverlay(null);
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
                p -> this.getShapeIndexMap().getProcessor().getf(p.x, p.y));
            change = true;
            break;
          }
        }
      }
    } while (change);

    return spines;
  }

  protected AbstractCellFrame spineToCellFrame(Spine spine) {
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
    List<Point> line = Geometry.rasterizeLine(lineEnd, searchEnd);
    double threshold = (double) thresholdSlider.getValue();
    for (int index = 0; index < line.size(); index++) {
      Point point = line.get(index);
      if (this.getShapeIndexMap().getProcessor().getf(point.x, point.y) <= threshold) {
        return index > 0 ? line.get(index - 1) : lineEnd;
      }
    }
    return lineEnd;
  }

  @Override
  public void imageUpdated() {
    if (this.initialized) {
      AbstractSkeletonBasedStep.cache.updateCache(this.imp);
      this.removeImageOverlay();
      if (this.skeletonCheckBox.isSelected()) {
        showImageOverlay(this.getSkeleton().toImagePlus());
      } else if (this.shapeIndexCheckBox.isSelected()) {
        showImageOverlay(getShapeIndexMap());
      }
    }
  }

  private class SkeletonPluginCache {
    private int impId;
    private LinkedHashMap<String, ImagePlus> indexMaps = new LinkedHashMap<String, ImagePlus>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, ImagePlus> eldest) {
        return this.size() > 15;
      }
    };
    private LinkedHashMap<String, Skeleton> skeletons = new LinkedHashMap<String, Skeleton>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Skeleton> eldest) {
        return this.size() > 15;
      }
    };
    public int slice;
    public int channel;
    public int frame;

    public SkeletonPluginCache(ImagePlus originalImp) {
      this.impId = originalImp.getID();
    }

    public boolean isTarget(ImagePlus originalImp) {
      return this.impId == originalImp.getID();
    }

    public void updateCache(ImagePlus preview) {
      this.slice = preview.getSlice();
      this.channel = preview.getChannel();
      this.frame = preview.getT();
    }

    public ImagePlus getIndexMap(String id) {
      return this.indexMaps.get(id);
    }

    public void setIndexMap(String id, ImagePlus imp) {
      this.indexMaps.put(id, imp);
    }

    public Skeleton getSkeleton(String id) {
      return this.skeletons.get(id);
    }

    public void setSkeleton(String id, Skeleton skeleton) {
      this.skeletons.put(id, skeleton);
    }
  }
}
