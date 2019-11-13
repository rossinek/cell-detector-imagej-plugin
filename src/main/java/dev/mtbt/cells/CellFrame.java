package dev.mtbt.cells;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.ListIterator;

import dev.mtbt.util.Pair;

abstract public class CellFrame {
  abstract public Point2D getBegin();

  abstract public Point2D getEnd();

  abstract public List<Point2D> toPolyline();

  public double getLength() {
    List<Point2D> polyline = this.toPolyline();
    return polyline.stream().skip(1)
        .reduce(new Pair<Point2D, Double>(polyline.get(0), 0.0),
            (acc, point) -> new Pair<>(point, acc.getValue() + acc.getKey().distance(point)),
            (v0, v1) -> new Pair<>(v1.getKey(), v0.getValue() + v1.getValue()))
        .getValue();
  }

  public Point2D pointAlongLine(double ratio) {
    double length = this.getLength();
    if (length == 0 || ratio < 0 || ratio > 1)
      return null;
    double targetDistance = length * ratio;
    ListIterator<Point2D> iterator = this.toPolyline().listIterator();
    double distance = 0.0;
    Point2D point, lastPoint = iterator.next();
    while (iterator.hasNext()) {
      point = iterator.next();
      double endDistance = distance + lastPoint.distance(point);
      if (endDistance >= targetDistance) {
        double deltaDistance = length * ratio - distance;
        double distanceRatio = deltaDistance / lastPoint.distance(point);
        double x = lastPoint.getX() + distanceRatio * (point.getX() - lastPoint.getX());
        double y = lastPoint.getY() + distanceRatio * (point.getY() - lastPoint.getY());
        return new Point2D.Double(x, y);
      }
      distance = endDistance;
      lastPoint = point;
    }
    return null;
  }
}
