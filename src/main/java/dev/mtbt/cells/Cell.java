package dev.mtbt.cells;

import java.util.ArrayList;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class Cell<CellFrame extends ICellFrame> {
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
    return this.frames.get(index - this.f0);
  }

  public boolean pushFrame(CellFrame frame) {
    return this.frames.add(frame);
  }

  public CellFrame setFrame(int index, CellFrame frame) {
    return this.frames.set(index - this.f0, frame);
  }

  public PolygonRoi toRoi(int index) {
    return new PolygonRoi(this.getFrame(index).toPolyline(), Roi.POLYLINE);
  }
}
