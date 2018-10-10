package dev.mtbt.cells;

import dev.mtbt.cells.skeleton.Spine;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class Cell {
  private Spine spine;

  public Cell (Spine spine) {
    this.spine = spine;
  }

  public PolygonRoi toRoi () {
    return new PolygonRoi(spine.toPolyLine(), Roi.POLYLINE);
  }
}
