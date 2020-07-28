package dev.mtbt.cells.skeleton;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import dev.mtbt.cells.AbstractCellFrame;

public class PolylineCellFrame extends AbstractCellFrame {
  private static final long serialVersionUID = 3215921062922697449L;
  private ArrayList<Point2D> polyline;

  public PolylineCellFrame(List<Point2D> polyline) {
    this.assignPolyline(polyline);
  }

  @Override
  public Point2D getBegin() {
    return polyline.get(0);
  }

  @Override
  public Point2D getEnd() {
    return polyline.get(polyline.size() - 1);
  }

  @Override
  public List<Point2D> toPolyline() {
    return new ArrayList<>(this.polyline);
  }

  @Override
  public void fitPolyline(List<Point2D> polyline) {
    this.assignPolyline(polyline);
  }

  @Override
  public AbstractCellFrame clone() {
    return new PolylineCellFrame(this.polyline);
  }

  private void assignPolyline(List<Point2D> polyline) {
    this.polyline = new ArrayList<>(polyline);
  }

  public void moveBegin(Point2D point) {
    polyline.set(0, point);
  }

  public void moveEnd(Point2D point) {
    polyline.set(polyline.size() - 1, point);
  }
}
