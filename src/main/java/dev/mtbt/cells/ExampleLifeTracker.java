package dev.mtbt.cells;

import dev.mtbt.gui.RunnableButton;
import dev.mtbt.imagej.HyperstackHelper;
import dev.mtbt.vendor.shapeindex.ShapeIndexMap;
import ij.ImagePlus;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class ExampleLifeTracker implements ICellsPluginStep {
  ImagePlus imp;
  CellCollection cellCollection;

  @Override
  public JComponent init(ImagePlus imp, CellCollection cellCollection) {
    this.imp = imp;
    this.cellCollection = cellCollection;

    JPanel component = new JPanel();
    component.setLayout(new BoxLayout(component, BoxLayout.Y_AXIS));
    component.add(new RunnableButton("duplicate", this::onDuplicateClick));
    return component;
  }

  @Override
  public void imageUpdated() {
    ImagePlus frame = HyperstackHelper.extractFrame(imp);
    ImagePlus shapeIndexMap = ShapeIndexMap.getShapeIndexMap(frame, 4.0);
    ImageRoi roi = new ImageRoi(0, 0, shapeIndexMap.getProcessor());
    Overlay overlay = new Overlay();
    overlay.add(roi);
    imp.setOverlay(overlay);
  }

  @Override
  public void cleanup() {
    imp.setOverlay(null);
  }

  void onDuplicateClick() {
    int currentFrameIndex = imp.getT();
    // get list of cells visible at current frame
    List<Cell> cells = cellCollection.getCells(currentFrameIndex);
    int nextFrameIndex = currentFrameIndex + 1;
    if (cells.size() < 1 || nextFrameIndex > imp.getNFrames()) {
      return;
    }
    // clear previous selections for next frames
    cells.forEach(cell -> cell.clearFuture(nextFrameIndex));
    cells.forEach(cell -> {
      // clone current selection
      AbstractCellFrame duplicate = cell.getFrame(currentFrameIndex).clone();
      // set selection for next frame to cloned value
      cell.setFrame(nextFrameIndex, duplicate);
    });
    // show next frame
    imp.setT(nextFrameIndex);
    // update image and cells preview
    imp.updateAndDraw();
  }
}
