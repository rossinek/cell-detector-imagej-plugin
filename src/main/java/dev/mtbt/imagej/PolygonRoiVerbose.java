package dev.mtbt.imagej;

import ij.gui.PolygonRoi;
import ij.gui.RoiListener;

public class PolygonRoiVerbose extends PolygonRoi {
  public PolygonRoiVerbose(float[] xPoints, float[] yPoints, int type) {
    super(xPoints, yPoints, type);
  }

  @Override
  public void mouseDownInHandle(int handle, int sx, int sy) {
    super.mouseDownInHandle(handle, sx, sy);
    this.notifyListeners(RoiListener.MODIFIED);
  }
}
