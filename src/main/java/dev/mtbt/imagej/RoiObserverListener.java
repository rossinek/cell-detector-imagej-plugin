package dev.mtbt.imagej;

import ij.gui.Roi;

public interface RoiObserverListener {
  public void roiModified(Roi roi, int id);
}
