package dev.mtbt.cells.skeleton;

import dev.mtbt.ImageJUtils;
import dev.mtbt.cells.Cell;
import dev.mtbt.cells.CellDetector;
import dev.mtbt.cells.skeleton.Spine;
import ij.gui.PolygonRoi;
import ij.gui.Toolbar;
import ij.plugin.frame.RoiManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

@Plugin(type = Command.class, menuPath = "Developement>Skeleton>Cell Detector")
public class SkeletonCellDetector extends SkeletonPlugin implements Initializable, CellDetector {

  @Parameter(label = "Select cells", callback = "onSelectCellsClick")
  private Button selectCellsButton;

  @Parameter(label = "Clear points", callback = "onClearPointsClick")
  private Button clearPointsButton;

  @Parameter(label = "Clear selected cells", callback = "onClearSelectedCellsClick")
  private Button clearSelectedCellsButton;

  @Parameter(label = "Run", callback = "onRunClick")
  private Button runButton;

  @Parameter(label = "Done", callback = "onDoneClick")
  protected Button doneButton;

  @Parameter(type = ItemIO.OUTPUT)
  private List<Cell> cells = new ArrayList<>();

  CompletableFuture<List<Cell>> result = new CompletableFuture<>();

  @Override
  public Future<List<Cell>> output() {
    return this.result;
  }

  protected void onSelectCellsClick() {
    if (this.impIndexMap != null) {
      this.impIndexMap.deleteRoi();
    }
    Toolbar.getInstance().setTool(Toolbar.POINT);
  }

  protected void onClearPointsClick() {
    if (this.impIndexMap != null) {
      this.impIndexMap.deleteRoi();
    }
  }

  protected void onClearSelectedCellsClick() {
    this.cells.clear();
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.runCommand("show all");
  }

  protected void onRunClick() {
    PolygonRoi roi = this.impIndexMap != null ? (PolygonRoi) this.impIndexMap.getRoi() : null;
    if (roi == null) {
      this.uiService.showDialog("There are no points selected.");
      return;
    }

    List<Spine> spines = this.performSearch(this.collectSelectedPoints());
    spines.forEach((spine) -> cells.add(new Cell(this.frameInput, new SpineCellFrame(spine))));

    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    cells.forEach(cell -> roiManager.addRoi(cell.toRoi(this.frameInput)));
    roiManager.runCommand("show all");
  }

  protected void onDoneClick() {
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.close();

    result.complete(this.cells);

    this.close();
  }

  private List<Point> collectSelectedPoints() {
    ArrayList<Point> points = new ArrayList<>();
    PolygonRoi roi = this.impIndexMap != null ? (PolygonRoi) this.impIndexMap.getRoi() : null;
    if (roi != null) {
      System.out.println("> collect selected points (" + roi.getFloatPolygon().npoints + ")");
      int[] xPoints = roi.getPolygon().xpoints;
      int[] yPoints = roi.getPolygon().ypoints;
      for (int i = 0; i < xPoints.length; i++) {
        points.add(new Point(xPoints[i], yPoints[i]));
      }
    }
    return points;
  }
}
