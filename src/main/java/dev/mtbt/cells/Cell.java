package dev.mtbt.cells;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.NotImplementedException;
import dev.mtbt.Utils;
import dev.mtbt.imagej.PolygonRoiVerbose;
import dev.mtbt.util.Pair;
import ij.IJ;
import ij.gui.Line;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.FloatPolygon;

public class Cell extends AbstractCellCollection {
  private static final long serialVersionUID = 1966938599823606758L;

  protected static final String PROPERTY_CELL_FRAME_ID = "cell-frame-id";

  private String name = null;

  protected ArrayList<CellFrame> frames = new ArrayList<>();

  private int f0;

  private Cell[] children = new Cell[] {};

  /**
   * Hashtable<cellFrameId, roiHashCode> where cellFrameId is unique identifier of frame and
   * roiHashCode is hash that represents PolygonRoi shape and will change each time roi shape
   * changed
   */
  private transient Hashtable<String, Integer> lastRoiHashCodes = new Hashtable<>();

  public Cell(int f0, CellFrame first) {
    this.f0 = f0;
    this.frames.add(first);
  }

  public void setFamily(String family) {
    this.setName(family, 0, 1);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.lastRoiHashCodes = new Hashtable<>();
  }

  protected void setName(String family, int generation, int indexInGeneration) {
    this.name = family + "-" + generation + "-" + indexInGeneration;
  }

  public String getName() {
    return this.name;
  }

  public String getFamily() {
    String[] parts = this.name.split("-");
    return parts[0];
  }

  public int getGeneration() {
    String[] parts = this.name.split("-");
    return Integer.parseInt(parts[1]);
  }

  public int getIndexInGeneration() {
    String[] parts = this.name.split("-");
    return Integer.parseInt(parts[2]);
  }

  public int getF0() {
    return this.f0;
  }

  public int getFN() {
    return this.f0 + this.frames.size();
  }

  public ArrayList<CellFrame> getFrames() {
    return this.frames;
  }

  public CellFrame getFrame(int index) {
    try {
      CellFrame frame = this.frames.get(index - this.f0);
      return frame;
    } catch (IndexOutOfBoundsException e) {
      return null;
    }
  }

  public boolean pushFrame(CellFrame frame) {
    return this.frames.add(frame);
  }

  public CellFrame setFrame(int index, CellFrame frame) {
    if (index - this.f0 == this.frames.size()) {
      this.pushFrame(frame);
    }
    return this.frames.set(index - this.f0, frame);
  }

  public void clearFuture(int fromIndex) throws IllegalArgumentException {
    if (fromIndex < this.getF0()) {
      throw new IllegalArgumentException("fromIndex has to be in the future");
    }
    if (fromIndex == this.getF0()) {
      this.destroy();
    } else if (fromIndex <= this.getFN()) {
      this.destroyChildren();
      this.frames.subList(fromIndex - this.f0, this.frames.size()).clear();
    } else {
      for (Cell child : children) {
        child.clearFuture(fromIndex);
      }
    }
  }

  public PolygonRoi getObservedRoi(int index) {
    CellFrame frame = this.getFrame(index);
    if (frame == null) {
      return null;
    }
    PolygonRoi roi = this.toRoi(frame);
    if (this.name != null) {
      roi.setName(this.getName());
    }
    roi.setProperty(PROPERTY_CELL_FRAME_ID, this.getCellFrameId(index));
    String cellFrameId = this.getCellFrameId(index);
    this.lastRoiHashCodes.put(cellFrameId, this.getPolygonRoiHashCode(roi));
    return roi;
  }

  public List<Roi> endsToRois(int index) {
    CellFrame frame = this.getFrame(index);
    if (frame == null)
      return new ArrayList<>();
    PointRoi beginRoi = new PointRoi(frame.getBegin().getX(), frame.getBegin().getY());
    PointRoi endRoi = new PointRoi(frame.getEnd().getX(), frame.getEnd().getY());
    beginRoi.setStrokeColor(Color.GREEN);
    endRoi.setStrokeColor(Color.RED);
    beginRoi.setName("");
    endRoi.setName("");
    return new ArrayList<>(Arrays.asList(new Roi[] {beginRoi, endRoi}));
  }

  public void setChildren(Cell c1, Cell c2) {
    if (c1.getF0() != this.getFN() || c2.getF0() != this.getFN())
      throw new IllegalArgumentException();

    c1.setParentCollection(this);
    c2.setParentCollection(this);
    this.children = new Cell[] {c1, c2};

    if (this.name != null) {
      c1.setName(this.getFamily(), this.getGeneration() + 1,
          (this.getIndexInGeneration() - 1) * 2 + 1);
      c2.setName(this.getFamily(), this.getGeneration() + 1,
          (this.getIndexInGeneration() - 1) * 2 + 2);
    }
  }

  public void destroyChildren() {
    for (Cell cell : this.children) {
      cell.setParentCollection(null);
      cell.destroy();
    }
    this.children = new Cell[] {};
  }

  public Cell[] getChildren() {
    return this.children;
  }

  /**
   * Returns descendants alive at frame `index`
   */
  public List<Cell> evolve(int index) {
    List<Cell> cells = new ArrayList<>();
    if (index < this.f0) {
      return cells;
    }
    cells.add(this);
    for (int i = this.f0; i <= index; i++) {
      final int currentIndex = i;
      cells = cells.stream().flatMap(cell -> {
        if (currentIndex < cell.getFN())
          return Arrays.stream(new Cell[] {cell});
        return Arrays.stream(cell.getChildren());
      }).collect(Collectors.toList());
    }
    return cells;
  }

  private String getCellFrameId(int index) {
    return this.hashCode() + ":" + index;
  }

  protected int getIndexByCellFrameId(String cellFrameId) {
    String[] parts = cellFrameId.split(":");
    return Integer.parseInt(parts[parts.length - 1]);
  }

  public PolygonRoi toRoi(int index) {
    CellFrame frame = this.getFrame(index);
    if (frame == null) {
      return null;
    }
    return this.toRoi(frame);
  }

  private PolygonRoi toRoi(CellFrame frame) {
    if (frame == null)
      return null;
    List<Point2D> polyline = frame.toPolyline();
    float[] xPoints = new float[polyline.size()];
    float[] yPoints = new float[polyline.size()];
    for (int i = 0; i < polyline.size(); i++) {
      xPoints[i] = (float) polyline.get(i).getX();
      yPoints[i] = (float) polyline.get(i).getY();
    }
    PolygonRoi polylineRoi = new PolygonRoiVerbose(xPoints, yPoints, Roi.POLYLINE);
    return polylineRoi;
  }

  public void cellFrameRoiModified(Roi modifiedRoi) {
    if (this.isOwnCellFrameRoi(modifiedRoi)) {
      PolygonRoi polygonRoi = (PolygonRoi) modifiedRoi;
      String cellFrameId = polygonRoi.getProperty(PROPERTY_CELL_FRAME_ID);
      int lastRoiHashCode = this.lastRoiHashCodes.get(cellFrameId);
      if (lastRoiHashCode != this.getPolygonRoiHashCode(polygonRoi)) {
        int index = this.getIndexByCellFrameId(cellFrameId);
        this.getFrame(index).fitPolyline(Utils.toPolyline(polygonRoi.getFloatPolygon()));
      }
    }
  }

  protected void cellFrameRoisCut(List<PolygonRoi> ownRois, Line line) {
    List<Pair<PolygonRoi, List<Point2D>[]>> changes = new ArrayList<>();
    Point2D l1 = new Point2D.Double(line.x1d, line.y1d);
    Point2D l2 = new Point2D.Double(line.x2d, line.y2d);
    ownRois.forEach(roi -> {
      List<Point2D>[] polylines =
          Utils.cutPolyline(Utils.toPolyline(roi.getFloatPolygon()), l1, l2);
      if (polylines.length > 1)
        changes.add(new Pair<>(roi, polylines));
    });
    changes.forEach(pair -> {
      String cellFrameId = pair.getKey().getProperty(PROPERTY_CELL_FRAME_ID);
      int index = this.getIndexByCellFrameId(cellFrameId);

      CellFrame cellFrame1 = this.getFrame(index);
      CellFrame cellFrame2 = cellFrame1.clone();
      cellFrame1.fitPolyline(pair.getValue()[0]);
      cellFrame2.fitPolyline(pair.getValue()[1]);
      Cell c1 = new Cell(index, cellFrame1);
      Cell c2 = new Cell(index, cellFrame2);
      if (index > this.getF0()) {
        this.clearFuture(index);
        this.setChildren(c1, c2);
      } else {
        try {
          this.parent.addToCollection(c1);
          this.parent.addToCollection(c2);
          this.destroy();
        } catch (Exception e) {
          IJ.showMessage(
              "Can't cut first frame of cell: Parent cell would have more than 2 children.");
        }
      }
      // update image to redraw rois
      line.getImage().updateAndDraw();
    });
  }

  protected void cellFrameRoisShorten(List<PolygonRoi> ownRois, ShapeRoi shape) {
    ownRois.forEach(roi -> {
      FloatPolygon fp = roi.getFloatPolygon();
      boolean containsBegin = shape.containsPoint(fp.xpoints[0], fp.ypoints[0]);
      boolean containsEnd =
          shape.containsPoint(fp.xpoints[fp.npoints - 1], fp.ypoints[fp.npoints - 1]);
      if (containsBegin || containsEnd) {
        List<Point2D> polyline = Utils.toPolyline(roi.getFloatPolygon());
        List<Point2D> newPolyline =
            Utils.erasePolylineEnd(polyline, shape, containsBegin ? Utils.BEGIN : Utils.END);

        String cellFrameId = roi.getProperty(PROPERTY_CELL_FRAME_ID);
        int index = this.getIndexByCellFrameId(cellFrameId);
        CellFrame cellFrame = this.getFrame(index);

        if (newPolyline != null && !newPolyline.isEmpty()) {
          cellFrame.fitPolyline(newPolyline);
        } else if (index > this.getF0()) {
          this.clearFuture(index);
        } else {
          this.destroy();
        }
        shape.getImage().updateAndDraw();
      }
    });
  }

  protected boolean isOwnCellFrameRoi(Roi roi) {
    if (roi == null) {
      return false;
    }
    String cellFrameId = roi.getProperty(PROPERTY_CELL_FRAME_ID);
    return cellFrameId != null && roi.getType() == Roi.POLYLINE
        && this.lastRoiHashCodes.containsKey(cellFrameId);
  }

  private int getPolygonRoiHashCode(PolygonRoi roi) {
    Polygon polygon = roi.getPolygon();
    String str = Arrays.toString(polygon.xpoints) + Arrays.toString(polygon.ypoints);
    return str.hashCode();
  }

  @Override
  public List<Cell> getCells(int index) {
    return this.evolve(index);
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public void addToCollection(AbstractCellCollection cellCollection) {
    throw new NotImplementedException(
        "Not implemented for Cell class – use `setChildren` instead.");
  }

  @Override
  public void removeFromCollection(AbstractCellCollection cell) {
    ArrayList<CellFrame> framesToPreserve;
    if (this.children[0] == cell) {
      framesToPreserve = children[1].frames;
    } else if (this.children[1] == cell) {
      framesToPreserve = children[0].frames;
    } else {
      throw new IllegalArgumentException();
    }
    this.frames.addAll(framesToPreserve);
    this.destroyChildren();
  }

  @Override
  public void destroy() {
    super.destroy();
    this.destroyChildren();
  }
}
