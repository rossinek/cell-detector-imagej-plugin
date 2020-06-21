package dev.mtbt.cells;

import javax.swing.JComponent;
import ij.ImagePlus;

public interface ICellsPluginStep {
  public JComponent init(ImagePlus imp, CellCollection cells);

  public void imageUpdated();

  public void cleanup();
}
