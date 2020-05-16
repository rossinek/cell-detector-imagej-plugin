package dev.mtbt.cells.skeleton;

import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import javax.swing.Box;
import javax.swing.JPanel;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import dev.mtbt.ImageJUtils;
import dev.mtbt.cells.Cell;
import dev.mtbt.cells.CellCollection;
import dev.mtbt.cells.ICellDetector;
import dev.mtbt.gui.RunnableButton;
import ij.gui.PolygonRoi;
import ij.gui.Toolbar;
import ij.plugin.frame.RoiManager;

@Plugin(type = Command.class, menuPath = "Development>Skeleton>Cell Detector")
public class SkeletonCellDetector extends SkeletonPlugin implements ICellDetector {

  private RunnableButton selectCellsButton;
  private RunnableButton clearSelectedCellsButton;
  private RunnableButton runButton;

  @Parameter(type = ItemIO.OUTPUT)
  private CellCollection cellCollection = new CellCollection();

  CompletableFuture<CellCollection> result = new CompletableFuture<>();

  @Override
  public void run() {
    if (!super.initComponents()) {
      return;
    }

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new FlowLayout());
    buttonsPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

    this.selectCellsButton = new RunnableButton("Select cells", this::onSelectCellsClick);
    buttonsPanel.add(selectCellsButton);
    this.runButton = new RunnableButton("Run detection", this::onRunClick);
    buttonsPanel.add(runButton);
    this.clearSelectedCellsButton = new RunnableButton("Reset", this::onResetClick);
    buttonsPanel.add(clearSelectedCellsButton);

    dialogContent.add(Box.createVerticalStrut(20));
    addCenteredComponent(dialogContent, buttonsPanel);

    this.dialog.pack();
    this.preview();
  }

  @Override
  public Future<CellCollection> output() {
    return this.result;
  }

  protected void onSelectCellsClick() {
    if (this.impPreview != null) {
      this.impPreview.deleteRoi();
    }
    Toolbar.getInstance().setTool(Toolbar.POINT);
  }

  protected void onResetClick() {
    if (this.impPreview != null) {
      this.impPreview.deleteRoi();
    }
    this.cellCollection = new CellCollection();
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.runCommand("show all");
  }

  protected void onRunClick() {
    PolygonRoi roi = this.impPreview != null ? (PolygonRoi) this.impPreview.getRoi() : null;
    if (roi == null) {
      this.uiService.showDialog("There are no points selected.");
      return;
    }

    List<Spine> spines = this.performSearch(this.collectSelectedPoints());
    int f0 = (int) this.frameSlider.getValue();
    for (int index = 0; index < spines.size(); index++) {
      char character = (char) ('A' + index);
      this.cellCollection
          .addToCollection(new Cell(f0, this.spineToCellFrame(spines.get(index)), "" + character));
    }

    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    cellCollection.getCells(f0)
        .forEach(cell -> roiManager.addRoi(cell.getObservedRoi((int) this.frameSlider.getValue())));
    roiManager.runCommand("show all");
  }

  protected void done() {
    super.done();
    result.complete(this.cellCollection);
  }

  private List<Point> collectSelectedPoints() {
    ArrayList<Point> points = new ArrayList<>();
    PolygonRoi roi = this.impPreview != null ? (PolygonRoi) this.impPreview.getRoi() : null;
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
