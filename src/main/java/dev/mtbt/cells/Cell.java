package dev.mtbt.cells;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class Cell {

  protected ArrayList<CellFrame> frames = new ArrayList<>();

  private int f0;

  private Cell[] children = new Cell[] {};

  public Cell(int f0, CellFrame first) {
    this.f0 = f0;
    this.frames.add(first);
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

  public PolygonRoi toRoi(int index) {
    CellFrame frame = this.getFrame(index);
    if (frame == null)
      return null;
    List<Point2D> polyline = frame.toPolyline();
    float[] xPoints = new float[polyline.size()];
    float[] yPoints = new float[polyline.size()];
    for (int i = 0; i < polyline.size(); i++) {
      xPoints[i] = (float) polyline.get(i).getX();
      yPoints[i] = (float) polyline.get(i).getY();
    }
    return new PolygonRoi(xPoints, yPoints, Roi.POLYLINE);
  }

  public void setChildren(Cell c1, Cell c2) {
    if (c1.getF0() != this.getFN() + 1 || c2.getF0() != this.getFN() + 1)
      throw new IllegalArgumentException();
    this.children = new Cell[] {c1, c2};
  }

  public Cell[] getChildren() {
    return this.children;
  }

  public List<Cell> evoluate(int index) {
    if (index < this.f0)
      throw new IllegalArgumentException();
    List<Cell> cells = new ArrayList<>();
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
}
