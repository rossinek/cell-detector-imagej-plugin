package dev.mtbt.cells.skeleton;

import dev.mtbt.ImageJUtils;
import dev.mtbt.Utils;
import dev.mtbt.cells.Cell;
import dev.mtbt.cells.CellCollection;
import dev.mtbt.cells.CellFrame;
import dev.mtbt.cells.ICellLifeTracker;
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
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Development>Skeleton>Cell Life Tracker")
public class SkeletonCellLifeTracker extends SkeletonPlugin implements ICellLifeTracker {

  private RunnableButton runButton;
  private RunnableSpinner nFramesSlider;

  CompletableFuture<Void> result = new CompletableFuture<>();

  @Override
  public void init(CellCollection cellCollection) {
    this.cellCollection = cellCollection;
    this.run();
    this.showFirstFrameWithCells();
  }

  @Override
  public void run() {
    if (this.cellCollection == null || !super.initComponents()) {
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

  @Override
  public Future<Void> output() {
    return this.result;
  }

  protected void showFirstFrameWithCells() {
    int frame = this.cellCollection.isEmpty() ? 1 : this.cellCollection.getF0();
    this.impPreviewStack.setT(frame);
  }

  protected void onRunClick() {
    if (this.cellCollection == null) {
      this.uiService.showDialog("There are no cells to track");
      return;
    }
    int f0 = this.impPreviewStack.getT();
    List<Cell> cells = this.cellCollection.getCells(f0);
    if (cells.size() < 1) {
      this.uiService.showDialog("There are no cells in current frame");
      return;
    }
    cells.forEach(cell -> cell.clearFuture(f0 + 1));
    for (int index = f0 + 1; index <= f0 + (int) this.nFramesSlider.getValue(); index++) {
      cells = this.cellCollection.getCells(index - 1);
      final int frameIndex = index;
      this.impPreviewStack.setT(frameIndex);
      this.preview();

      HashMap<Cell, List<Pair<Point2D, Spine>>> successors = new HashMap<>();

      cells.stream().forEach(cell -> {
        // generate spines for some points on previous spine frame
        // get generated spine that is closest to all points
        // add it as next frame to cell
        CellFrame frame = cell.getFrame(frameIndex - 1);
        List<Double> ratioCandidates = Arrays.asList(0.2, 0.4, 0.6, 0.8);
        List<Point2D> pointCandidates = ratioCandidates.stream()
            .map(ratio -> frame.pointAlongLine(ratio)).collect(Collectors.toList());
        Pair<Point2D, Spine> nextSpine = this.bestCandidateForNewSpine(pointCandidates);

        List<Pair<Point2D, Spine>> nextSpines = new ArrayList<>(Arrays.asList(nextSpine));

        int candidateIndex = pointCandidates.indexOf(nextSpine.getKey());
        double nextSpineLength = Utils.polylineLength(nextSpine.getValue().toPolyline());
        double prevSpineLength = frame.getLength();
        int nRatios = ratioCandidates.size();
        if (nextSpineLength < 0.9 * prevSpineLength) {
          List<Double> oppositeRatios;
          if ((double) candidateIndex >= nRatios / 2.0) {
            oppositeRatios = ratioCandidates.subList(0, nRatios / 2);
          } else {
            oppositeRatios = ratioCandidates.subList((int) Math.ceil(nRatios / 2.0), nRatios);
          }

          List<Point2D> oppositePointCandidates = oppositeRatios.stream()
              .map(ratio -> frame.pointAlongLine(ratio)).collect(Collectors.toList());
          Pair<Point2D, Spine> anotherNextSpine =
              this.bestCandidateForNewSpine(oppositePointCandidates);

          if (!nextSpine.getValue().equals(anotherNextSpine.getValue())) {
            nextSpines.add(anotherNextSpine);
            if ((double) candidateIndex < nRatios / 2.0) {
              Collections.reverse(nextSpines);
            }
          }
        }
        successors.put(cell, nextSpines);
      });

      this.fixConflicts(successors.values().stream().flatMap(l -> l.stream())
          .map(p -> new Pair<>(Utils.toAwtPoint(p.getKey()), p.getValue()))
          .collect(Collectors.toList()));

      successors.forEach((cell, list) -> {
        if (list.size() < 2) {
          CellFrame prevCellFrame = cell.getFrame(frameIndex - 1);
          this.ensureValidSuccessorDirection(prevCellFrame, list.get(0).getValue());
          cell.setFrame(frameIndex, this.spineToCellFrame(list.get(0).getValue()));
        } else {
          this.ensureValidSiblingsDirections(
              list.stream().map(Pair::getValue).collect(Collectors.toList()));
          Cell c1 = new Cell(frameIndex, this.spineToCellFrame(list.get(0).getValue()));
          Cell c2 = new Cell(frameIndex, this.spineToCellFrame(list.get(1).getValue()));
          cell.setChildren(c1, c2);
        }
      });
    }

    this.preview();
  }

  private void ensureValidSuccessorDirection(CellFrame cellFrame, Spine successor) {
    double bb = cellFrame.getBegin().distance(successor.getBegin());
    double be = cellFrame.getBegin().distance(successor.getEnd());
    double eb = cellFrame.getEnd().distance(successor.getBegin());
    double ee = cellFrame.getEnd().distance(successor.getEnd());
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

  private Pair<Point2D, Spine> bestCandidateForNewSpine(List<Point2D> pointCandidates) {
    return pointCandidates.stream().map(point2d -> {
      Point point = Utils.toAwtPoint(point2d);
      Spine spine = this.performSearch(Arrays.asList(point)).get(0);
      double score = pointCandidates.stream().reduce(0.0,
          (acc, candidate) -> acc + point2d.distance(candidate), (v0, v1) -> v0 + v1);
      return new Pair<>(new Pair<>(point2d, spine), score);
    }).min((s0, s1) -> Double.compare(s0.getValue(), s1.getValue())).get().getKey();
  }

  protected void done() {
    super.done();
    result.complete(null);
  }

  protected void updateAndDrawCells() {
    if (this.cellCollection != null) {
      RoiManager roiManager = ImageJUtils.getRoiManager();
      roiManager.reset();
      this.cellCollection.getCells(this.impPreviewStack.getT()).stream()
          .map(cell -> cell.getObservedRoi(this.impPreviewStack.getT())).filter(roi -> roi != null)
          .forEach(roi -> roiManager.addRoi(roi));
      roiManager.runCommand("show all");
    }
  }
}
