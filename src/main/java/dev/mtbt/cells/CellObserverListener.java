package dev.mtbt.cells;

import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

public interface CellObserverListener {
  public void cellFrameRoiModified(Roi modifiedRoi);

  public void cellFrameRoisCut(Line lineRoi);

  public void cellFrameRoisShorten(ShapeRoi shapeRoi);
}
