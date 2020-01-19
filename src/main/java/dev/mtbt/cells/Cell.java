package dev.mtbt.cells;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class Cell {
  private String name = null;

  protected ArrayList<CellFrame> frames = new ArrayList<>();

  private int f0;

  private Cell[] children = new Cell[] {};

  public Cell(int f0, CellFrame first) {
    this.f0 = f0;
    this.frames.add(first);
  }

  public Cell(int f0, CellFrame first, String family) {
    this(f0, first);
    this.setName(family, 0, 1);
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
    return this.f0 + this.frames.size() - 1;
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

  public void clearFuture(int fromIndex) {
    if (fromIndex <= this.getF0()) {
      throw new IllegalArgumentException("fromIndex has to be in the future");
    }
    if (fromIndex <= this.getFN() + 1) {
      this.removeChildren();
      this.frames.subList(fromIndex - this.f0, this.frames.size()).clear();
    } else {
      for (Cell child : children) {
        child.clearFuture(fromIndex);
      }
    }
  }

  public PolygonRoi toRoi(int index) {
    CellFrame frame = this.getFrame(index);
    if (frame == null)
      return null;
    return this.toRoi(frame);

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
    if (c1.getF0() != this.getFN() + 1 || c2.getF0() != this.getFN() + 1)
      throw new IllegalArgumentException();

    this.children = new Cell[] {c1, c2};

    if (this.name != null) {
      c1.setName(this.getFamily(), this.getGeneration() + 1,
          (this.getIndexInGeneration() - 1) * 2 + 1);
      c2.setName(this.getFamily(), this.getGeneration() + 1,
          (this.getIndexInGeneration() - 1) * 2 + 2);
    }
  }

  public void removeChildren() {
    this.children = new Cell[] {};
  }

  public Cell[] getChildren() {
    return this.children;
  }

  /**
   * Returns descendants alive at frame `index`
   */
  public List<Cell> evoluate(int index) {
    List<Cell> cells = new ArrayList<>();
    if (index < this.f0) {
      return cells;
    }
    cells.add(this);
    for (int i = this.f0; i <= index; i++) {
      final int currentIndex = i;
      cells = cells.stream().flatMap(cell -> {
        if (currentIndex <= cell.getFN())
          return Arrays.stream(new Cell[] {cell});
        return Arrays.stream(cell.getChildren());
      }).collect(Collectors.toList());
    }
    return cells;
  }

  public static List<Cell> evoluate(List<Cell> cells, int index) {
    return cells.stream().flatMap(cell -> cell.evoluate(index).stream())
        .collect(Collectors.toList());
  }

  PolygonRoi toRoi(CellFrame frame) {
    if (frame == null)
      return null;
    List<Point2D> polyline = frame.toPolyline();
    float[] xPoints = new float[polyline.size()];
    float[] yPoints = new float[polyline.size()];
    for (int i = 0; i < polyline.size(); i++) {
      xPoints[i] = (float) polyline.get(i).getX();
      yPoints[i] = (float) polyline.get(i).getY();
    }
    PolygonRoi polylineRoi = new PolygonRoi(xPoints, yPoints, Roi.POLYLINE);
    if (this.name != null)
      polylineRoi.setName(this.name);
    return polylineRoi;
  }
}
