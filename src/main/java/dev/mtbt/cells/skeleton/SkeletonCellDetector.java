package dev.mtbt.cells.skeleton;

import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JPanel;
import dev.mtbt.cells.Cell;
import dev.mtbt.cells.CellCollection;
import dev.mtbt.cells.ICellsPluginStep;
import dev.mtbt.gui.RunnableButton;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Toolbar;

public class SkeletonCellDetector extends SkeletonBasedStep implements ICellsPluginStep {

  private RunnableButton selectCellsButton;
  private RunnableButton clearSelectedCellsButton;
  private RunnableButton runButton;

  public SkeletonCellDetector() {
    super();
  }

  @Override
  public JPanel init(ImagePlus imp, CellCollection cellCollection) {
    super.init(imp, cellCollection);

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new FlowLayout());
    buttonsPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

    this.selectCellsButton = new RunnableButton("Select cells", this::onSelectCellsClick);
    buttonsPanel.add(selectCellsButton);
    this.runButton = new RunnableButton("Run detection", this::onRunClick);
    buttonsPanel.add(runButton);
    this.clearSelectedCellsButton = new RunnableButton("Reset", this::onResetClick);
    buttonsPanel.add(clearSelectedCellsButton);

    this.dialogContent.add(Box.createVerticalStrut(20));
    addCenteredComponent(this.dialogContent, buttonsPanel);

    // this.dialog.pack();
    this.preview();
    return this.dialogContent;
  }

  @Override
  public void imageUpdated() {
    super.imageUpdated();
  }

  @Override
  public void cleanup() {
    super.cleanup();
  }

  protected void onSelectCellsClick() {
    if (this.imp != null) {
      this.imp.deleteRoi();
    }
    Toolbar.getInstance().setTool(Toolbar.POINT);
  }

  protected void onResetClick() {
    if (this.imp != null) {
      this.imp.deleteRoi();
    }
    this.cellCollection.clear();
    this.updateAndDrawCells();
  }

  protected void onRunClick() {
    PolygonRoi roi = this.imp != null ? (PolygonRoi) this.imp.getRoi() : null;
    if (roi == null) {
      IJ.showMessage("There are no points selected.");
      return;
    }

    List<Spine> spines = this.performSearch(this.collectSelectedPoints());
    int f0 = this.imp.getT();
    for (int index = 0; index < spines.size(); index++) {
      this.cellCollection.addToCollection(new Cell(f0, this.spineToCellFrame(spines.get(index))));
    }

    this.updateAndDrawCells();
  }

  private List<Point> collectSelectedPoints() {
    ArrayList<Point> points = new ArrayList<>();
    PolygonRoi roi = this.imp != null ? (PolygonRoi) this.imp.getRoi() : null;
    if (roi != null) {
      int[] xPoints = roi.getPolygon().xpoints;
      int[] yPoints = roi.getPolygon().ypoints;
      for (int i = 0; i < xPoints.length; i++) {
        points.add(new Point(xPoints[i], yPoints[i]));
      }
    }
    return points;
  }
}
