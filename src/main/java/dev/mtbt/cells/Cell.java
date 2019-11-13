package dev.mtbt.cells;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class Cell {

  protected ArrayList<CellFrame> frames = new ArrayList<>();

  private int f0;

  public Cell(int f0, CellFrame first) {
    this.f0 = f0;
    this.frames.add(first);
  }

  public int getF0() {
    return this.f0;
  }

  public ArrayList<CellFrame> getFrames(int index) {
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
}
