package dev.mtbt.cells;

import java.util.List;
import ij.ImagePlus;

public interface CellObserverListener {
  public ImagePlus getObservedImage();

  public List<Cell> getActiveObservedCells();
}
