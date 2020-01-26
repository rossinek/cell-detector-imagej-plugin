package dev.mtbt.cells.skeleton;

import dev.mtbt.ImageJUtils;
import dev.mtbt.Utils;
import dev.mtbt.cells.Cell;
import dev.mtbt.cells.CellLifeTracker;
import dev.mtbt.gui.RunnableButton;
import dev.mtbt.gui.RunnableSpinner;
import dev.mtbt.util.Pair;
import ij.plugin.frame.RoiManager;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JLabel;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Development>Skeleton>Cell Life Tracker")
public class SkeletonCellLifeTracker extends SkeletonPlugin implements CellLifeTracker {

  private RunnableButton runButton;
  private RunnableSpinner nFramesSlider;

  @Parameter(type = ItemIO.OUTPUT)
  private List<Cell> cells = null;

  CompletableFuture<Void> result = new CompletableFuture<>();

  @Override
  public void init(List<Cell> cells) {
    this.cells = cells;
    this.run();
    this.showFirstFrameWithCells();
  }

  @Override
  public void run() {
    if (this.cells == null || !super.initComponents()) {
      return;
    }

    dialogContent.add(Box.createVerticalStrut(20));
    this.nFramesSlider = new RunnableSpinner(20, 1, 40, null);
    addCenteredComponent(dialogContent, new JLabel("Track life for (#frames):"));
    addCenteredComponent(dialogContent, nFramesSlider);

    dialogContent.add(Box.createVerticalStrut(20));
    this.runButton = new RunnableButton("Run!", this::onRunClick);
    addCenteredComponent(dialogContent, runButton);

    this.dialog.pack();
    this.preview();
  }

  public void preview() {
    super.preview();
    if (this.cells != null) {
      RoiManager roiManager = ImageJUtils.getRoiManager();
      roiManager.reset();
      Cell.evoluate(this.cells, (int) this.frameSlider.getValue()).stream()
          .map(cell -> cell.toRoi((int) this.frameSlider.getValue())).filter(roi -> roi != null)
          .forEach(roi -> roiManager.addRoi(roi));
      roiManager.runCommand("show all");
    }
  }

  @Override
  public Future<Void> output() {
    return this.result;
  }

  protected void showFirstFrameWithCells() {
    int frame = this.cells.isEmpty() ? 1
        : this.cells.stream().map(c -> c.getF0()).reduce(this.cells.get(0).getF0(), Integer::min);
    this.frameSlider.getModel().setValue(frame);
  }

  protected void onRunClick() {
    if (this.cells == null) {
      this.uiService.showDialog("There are no cells to track");
      return;
    }
    int f0 = (int) this.frameSlider.getValue();
    List<Cell> cells = Cell.evoluate(this.cells, f0);
    if (cells.size() < 1) {
      this.uiService.showDialog("There are no cells to in current frame");
      return;
    }
    cells.forEach(cell -> cell.clearFuture(f0 + 1));
    for (int index = f0 + 1; index <= f0 + (int) this.nFramesSlider.getValue(); index++) {
      cells = Cell.evoluate(cells, index - 1);
      final int frameIndex = index;
      this.frameSlider.setValue(frameIndex);
      this.preview();

      HashMap<Cell, List<Pair<Point2D, Spine>>> successors = new HashMap<>();

      cells.stream().forEach(cell -> {
        // generate spines for some points on previous spine frame
        // get generated spine that is closest to all points
        // add it as next frame to cell
        SpineCellFrame frame = (SpineCellFrame) cell.getFrame(frameIndex - 1);
        List<Double> ratioCandidates = Arrays.asList(0.1, 0.2, 0.4, 0.6, 0.8, 0.9);
        List<Point2D> pointCandidates = ratioCandidates.stream()
            .map(ratio -> frame.pointAlongLine(ratio)).collect(Collectors.toList());
        Pair<Point2D, Spine> nextSpine =
            this.bestCandidateForNewSpine(pointCandidates, frame.getSpine());

        List<Pair<Point2D, Spine>> nextSpines = new ArrayList<>(Arrays.asList(nextSpine));

        int candidateIndex = pointCandidates.indexOf(nextSpine.getKey());
        double nextSpineLength = Utils.polylineLength(nextSpine.getValue().toPolyline());
        double prevSpineLength = frame.getLength();
        int nratios = ratioCandidates.size();
        if (nextSpineLength < 0.9 * prevSpineLength) {
          List<Double> oppositeRatios;
          if ((double) candidateIndex >= nratios / 2.0) {
            oppositeRatios = ratioCandidates.subList(0, nratios / 2);
          } else {
            oppositeRatios = ratioCandidates.subList((int) Math.ceil(nratios / 2.0), nratios);
          }

          List<Point2D> oppositePointCandidates = oppositeRatios.stream()
              .map(ratio -> frame.pointAlongLine(ratio)).collect(Collectors.toList());
          Pair<Point2D, Spine> anotherNextSpine =
              this.bestCandidateForNewSpine(oppositePointCandidates, frame.getSpine());
          nextSpines.add(anotherNextSpine);

          if ((double) candidateIndex < nratios / 2.0) {
            Collections.reverse(nextSpines);
          }
        }
        successors.put(cell, nextSpines);
      });

      this.fixConflicts(successors.values().stream().flatMap(l -> l.stream())
          .map(p -> new Pair<>(Utils.toAwtPoint(p.getKey()), p.getValue()))
          .collect(Collectors.toList()));

      successors.forEach((cell, list) -> {
        if (list.size() < 2) {
          Spine prevSpine = ((SpineCellFrame) cell.getFrame(frameIndex - 1)).getSpine();
          this.ensureValidSuccessorDirection(prevSpine, list.get(0).getValue());
          cell.setFrame(frameIndex, new SpineCellFrame(list.get(0).getValue()));
        } else {
          this.ensureValidSiblingsDirections(
              list.stream().map(Pair::getValue).collect(Collectors.toList()));
          Cell c1 = new Cell(frameIndex, new SpineCellFrame(list.get(0).getValue()));
          Cell c2 = new Cell(frameIndex, new SpineCellFrame(list.get(1).getValue()));
          cell.setChildren(c1, c2);
        }
      });
    }

    this.preview();
  }

  private void ensureValidSuccessorDirection(Spine spine, Spine successor) {
    double bb = spine.getBegin().distance(successor.getBegin());
    double be = spine.getBegin().distance(successor.getEnd());
    double eb = spine.getEnd().distance(successor.getBegin());
    double ee = spine.getEnd().distance(successor.getEnd());
    double min = Math.min(bb, Math.min(be, Math.min(eb, ee)));

    if (bb != min && ee != min) {
      successor.reverse();
    }
  }

  // Ensure that closest vertices are called "end"
  private void ensureValidSiblingsDirections(List<Spine> list) {
    if (list.size() != 2)
      throw new IllegalArgumentException();
    Spine s1 = list.get(0);
    Spine s2 = list.get(1);

    double bb = s1.getBegin().distance(s2.getBegin());
    double be = s1.getBegin().distance(s2.getEnd());
    double eb = s1.getEnd().distance(s2.getBegin());
    double ee = s1.getEnd().distance(s2.getEnd());
    double min = Math.min(bb, Math.min(be, Math.min(eb, ee)));

    if (bb == min) {
      s1.reverse();
      s2.reverse();
    } else if (be == min) {
      s1.reverse();
    } else if (eb == min) {
      s2.reverse();
    }
  }

  private Pair<Point2D, Spine> bestCandidateForNewSpine(List<Point2D> pointCandidates,
      Spine previousSpine) {
    return pointCandidates.stream().map(point2d -> {
      Point point = Utils.toAwtPoint(point2d);
      Spine spine = this.performSearch(Arrays.asList(point), previousSpine).get(0);
      double score = pointCandidates.stream().reduce(0.0,
          (acc, candidate) -> acc + point2d.distance(candidate), (v0, v1) -> v0 + v1);
      return new Pair<>(new Pair<>(point2d, spine), score);
    }).min((s0, s1) -> Double.compare(s0.getValue(), s1.getValue())).get().getKey();
  }

  protected void done() {
    super.done();
    result.complete(null);
  }
}
