package dev.mtbt.imagej;

import ij.gui.Roi;

public interface IRoiObserverListener {
  public void roiModified(Roi roi, int id);
}
