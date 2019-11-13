package dev.mtbt.cells.skeleton;

import dev.mtbt.cells.CellFrame;

import java.awt.geom.Point2D;
import java.util.*;

public class SpineCellFrame extends CellFrame {
  private Spine spine;

  public SpineCellFrame(Spine s) {
    this.spine = s;
  }

  @Override
  public List<Point2D> toPolyline() {
    return this.spine.toPolyline();
  }

  @Override
  public Point2D getBegin() {
    return this.spine.getBegin();
  }

  @Override
  public Point2D getEnd() {
    return this.spine.getEnd();
  }

  public Spine getSpine() {
    return this.spine;
  }
}
