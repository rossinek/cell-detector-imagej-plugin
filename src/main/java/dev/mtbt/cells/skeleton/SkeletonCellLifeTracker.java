package dev.mtbt.cells.skeleton;

import dev.mtbt.util.Geometry;
import dev.mtbt.cells.Cell;
import dev.mtbt.cells.CellCollection;
import dev.mtbt.cells.AbstractCellFrame;
import dev.mtbt.gui.RunnableButton;
import dev.mtbt.gui.RunnableSpinner;
import dev.mtbt.util.Pair;
import ij.IJ;
import ij.ImagePlus;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JPanel;

public class SkeletonCellLifeTracker extends AbstractSkeletonBasedStep {

  private RunnableButton previousFrameButton;
  private RunnableButton duplicateNextFrameButton;
  private RunnableButton calculateNextFramesButton;
  private RunnableSpinner nFramesSpinner;

  CompletableFuture<Void> result = new CompletableFuture<>();

  @Override
  public JPanel init(ImagePlus imp, CellCollection cellCollection) {
    super.init(imp, cellCollection);

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new FlowLayout());
    buttonsPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

    this.previousFrameButton = new RunnableButton("< previous", this::onPreviousFrameClick);
    buttonsPanel.add(this.previousFrameButton);

    this.nFramesSpinner = new RunnableSpinner(1, 1, 20, () -> {
      int n = (int) this.nFramesSpinner.getValue();
      this.calculateNextFramesButton
          .setText(n > 1 ? ("calculate next " + n + " >") : "calculate next >");
    });
    buttonsPanel.add(this.nFramesSpinner);

    this.calculateNextFramesButton =
        new RunnableButton("calculate next >", this::onCalculateNextFramesClick);
    buttonsPanel.add(this.calculateNextFramesButton);

    this.duplicateNextFrameButton =
        new RunnableButton("duplicate >", this::onDuplicateNextFrameClick);
    buttonsPanel.add(this.duplicateNextFrameButton);

    dialogContent.add(Box.createVerticalStrut(20));
    addCenteredComponent(dialogContent, buttonsPanel);

    // this.dialog.pack();
    this.showFirstFrameWithCells();

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

  protected void showFirstFrameWithCells() {
    int frame = this.cellCollection.isEmpty() ? 1 : this.cellCollection.getF0();
    this.imp.setT(frame);
    this.preview();
  }

  protected void onPreviousFrameClick() {
    int frame = this.imp.getT();
    if (frame > 1) {
      this.imp.setT(frame - 1);
      this.preview();
    }
  }

  protected void onDuplicateNextFrameClick() {
    int f0 = this.imp.getT();
    List<Cell> cells = this.cellCollection.getCells(f0);
    if (cells.size() < 1) {
      IJ.showMessage("There are no cells in current frame");
      return;
    }
    cells.forEach(cell -> cell.clearFuture(f0 + 1));
    int index = f0 + 1;
    if (index > this.imp.getNFrames()) {
      return;
    }
    cells.forEach(cell -> cell.setFrame(index, cell.getFrame(index - 1).clone()));
    this.imp.setT(index);
    this.preview();
  }

  protected void onCalculateNextFramesClick() {
    for (int i = 0; i < (int) this.nFramesSpinner.getValue(); i++) {
      this.calculateNextFrame();
    }
  }

  protected void calculateNextFrame() {
    int f0 = this.imp.getT();
    List<Cell> cells = this.cellCollection.getCells(f0);
    if (cells.size() < 1) {
      IJ.showMessage("There are no cells in current frame");
      return;
    }
    final int frameIndex = f0 + 1;
    cells.forEach(cell -> cell.clearFuture(frameIndex));
    if (frameIndex > this.imp.getNFrames()) {
      return;
    }
    cells = this.cellCollection.getCells(frameIndex - 1);
    this.imp.setT(frameIndex);

    this.preview();

    HashMap<Cell, List<Pair<Point2D, Spine>>> successors = new HashMap<>();
    cells.stream().forEach(cell -> {
      // generate spines for some points on previous spine frame
      // get generated spine that is closest to all points
      // add it as next frame to cell
      AbstractCellFrame frame = cell.getFrame(frameIndex - 1);
      List<Double> ratioCandidates = Arrays.asList(0.2, 0.4, 0.6, 0.8);
      List<Point2D> pointCandidates = ratioCandidates.stream()
          .map(ratio -> frame.pointAlongLine(ratio)).collect(Collectors.toList());
      Pair<Point2D, Spine> nextSpine = this.bestCandidateForNewSpine(pointCandidates);
      List<Pair<Point2D, Spine>> nextSpines = new ArrayList<>(Arrays.asList(nextSpine));

      int candidateIndex = pointCandidates.indexOf(nextSpine.getKey());
      double nextSpineLength = Geometry.polylineLength(nextSpine.getValue().toPolyline());
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
        .map(p -> new Pair<>(Geometry.toAwtPoint(p.getKey()), p.getValue()))
        .collect(Collectors.toList()));

    successors.forEach((cell, list) -> {
      if (list.size() < 2) {
        AbstractCellFrame prevCellFrame = cell.getFrame(frameIndex - 1);
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
    this.preview();
  }

  private void ensureValidSuccessorDirection(AbstractCellFrame cellFrame, Spine successor) {
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
      Point point = Geometry.toAwtPoint(point2d);
      Spine spine = this.performSearch(Arrays.asList(point)).get(0);
      double score = pointCandidates.stream().reduce(0.0,
          (acc, candidate) -> acc + point2d.distance(candidate), (v0, v1) -> v0 + v1);
      return new Pair<>(new Pair<>(point2d, spine), score);
    }).min((s0, s1) -> Double.compare(s0.getValue(), s1.getValue())).get().getKey();
  }
}
