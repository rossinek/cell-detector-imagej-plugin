package dev.mtbt.cells;

import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.ShapeRoi;

public interface CellObserverListener {
  public void cellFrameRoiModified(PolygonRoi modifiedRoi);

  public void cellFrameRoisCut(Line lineRoi);

  public void cellFrameRoisShorten(ShapeRoi shapeRoi);
}
