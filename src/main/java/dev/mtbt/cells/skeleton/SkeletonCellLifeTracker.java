package dev.mtbt.cells.skeleton;

import dev.mtbt.ImageJUtils;
import dev.mtbt.cells.Cell;
import dev.mtbt.cells.CellLifeTracker;
import dev.mtbt.util.Pair;
import ij.plugin.frame.RoiManager;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

@Plugin(type = Command.class, menuPath = "Developement>Skeleton>Cell Life Tracker")
public class SkeletonCellLifeTracker extends SkeletonPlugin
    implements Initializable, CellLifeTracker {

  @Parameter(label = "Run", callback = "onRunClick")
  private Button runButton;

  @Parameter(label = "Done", callback = "onDoneClick")
  protected Button doneButton;

  @Parameter(type = ItemIO.OUTPUT)
  private List<Cell> cells = null;

  CompletableFuture<Void> result = new CompletableFuture<>();

  @Override
  public void init(List<Cell> cells) {
    this.cells = cells;
  }

  @Override
  public void run() {
    super.run();

    if (this.cells != null) {
      RoiManager roiManager = ImageJUtils.getRoiManager();
      roiManager.reset();
      this.cells.stream().map(cell -> cell.toRoi(this.frameInput)).filter(roi -> roi != null)
          .forEach(roi -> roiManager.addRoi(roi));
      roiManager.runCommand("show all");
    }
  }

  @Override
  public Future<Void> output() {
    return this.result;
  }

  protected void onRunClick() {
    if (this.cells == null || this.cells.size() < 1) {
      this.uiService.showDialog("There are no cells to track");
      return;
    }
    int f0 = this.cells.get(0).getF0();
    if (!this.cells.stream().allMatch(c -> c.getF0() == f0)) {
      this.uiService.showDialog("Not implemented for different f0s");
    }


    for (int index = f0 + 1; index <= f0 + 40; index++) {
      final int frameIndex = index;
      this.frameInput = frameIndex;
      this.run();

      List<Pair<Point, Spine>> nextSpines = this.cells.stream().map(cell -> {
        // generate some spines for points (0, 0.2, 0.4, 0.6, 0.8, 1)
        // get generated spine that is closest to all points
        // add it as next frame to cell
        SpineCellFrame frame = (SpineCellFrame) cell.getFrame(frameIndex - 1);
        List<Point2D> pointCandidates = Arrays.asList(0.0, 0.5, 1.0).stream()
            .map(ratio -> frame.pointAlongLine(ratio)).collect(Collectors.toList());
        Pair<Point, Spine> nextSpine = pointCandidates.stream()
            .map(p -> new Point((int) Math.round(p.getX()), (int) Math.round(p.getY())))
            .map(point -> new Pair<>(point, this.performSearch(Arrays.asList(point)).get(0)))
            .map(candidate -> {
              double score = pointCandidates.stream().map(dev.mtbt.graph.Point::new).reduce(0.0,
                  (acc, point) -> acc + candidate.getValue().distance(point), (v0, v1) -> v0 + v1);
              return new Pair<>(candidate, score);
            }).min((s0, s1) -> Double.compare(s0.getValue(), s1.getValue())).get().getKey();
        return nextSpine;
      }).collect(Collectors.toList());

      this.fixConflicts(nextSpines);

      for (int i = 0; i < this.cells.size(); i++) {
        this.cells.get(i).setFrame(frameIndex, new SpineCellFrame(nextSpines.get(i).getValue()));
      }
    }

    this.run();
  }

  protected void onDoneClick() {
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.close();

    result.complete(null);

    this.close();
  }
}
