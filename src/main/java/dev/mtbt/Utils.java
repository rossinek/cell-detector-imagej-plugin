package dev.mtbt;

import dev.mtbt.graph.*;
import dev.mtbt.util.Pair;
import ij.gui.ShapeRoi;
import ij.process.FloatPolygon;
import java.util.*;
import java.util.function.Predicate;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class Utils {
  public final static int BEGIN = 0;
  public final static int END = 1;

  public static double distance(Point p0, Point p1) {
    return Math.sqrt(((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y)));
  }

  public static double distance(Point point, Set<Point> points) {
    double bestDistance = Double.NEGATIVE_INFINITY;
    for (Point p : points) {
      double dist = distance(point, p);
      if (dist < bestDistance)
        bestDistance = dist;
    }
    return bestDistance;
  }

  /**
   * Perpendicular distance between line and point
   *
   * @param l1 first point on line
   * @param l2 second point on line
   * @param p  point
   * @return
   */
  public static double perpendicularDistance(Point l1, Point l2, Point p) {
    double dy = l2.y - l1.y;
    double dx = l2.x - l1.x;
    double numerator = Math.abs(dy * p.x - dx * p.y + l2.x * l1.y - l2.y * l1.x);
    double denominator = Math.sqrt(dy * dy + dx * dx);
    return numerator / denominator;
  }

  /**
   * Simplify geometry using the Ramer–Douglas–Peucker algorithm.
   *
   * @param polyLine  poly line to simplify
   * @param tolerance tolerance of simplification
   * @return simplified poly line
   */
  public static ArrayList<Point> simplifyPolylineOneWay(List<Point> polyLine, double tolerance) {
    double maxDistance = 0;
    int maxIndex = 0;

    for (int i = 1; i <= polyLine.size() - 2; i++) {
      Point l1 = polyLine.get(0);
      Point l2 = polyLine.get(polyLine.size() - 1);
      Point point = polyLine.get(i);
      double dist = perpendicularDistance(l1, l2, point);
      if (dist > maxDistance) {
        maxIndex = i;
        maxDistance = dist;
      }
    }

    ArrayList<Point> results = new ArrayList<>();
    if (maxDistance > tolerance) {
      results.addAll(simplifyPolylineOneWay(polyLine.subList(0, maxIndex), tolerance));
      results
          .addAll(simplifyPolylineOneWay(polyLine.subList(maxIndex, polyLine.size()), tolerance));
    } else if (polyLine.size() > 1) {
      results.add(polyLine.get(0));
      results.add(polyLine.get(polyLine.size() - 1));
    } else {
      results.add(polyLine.get(0));
    }
    return results;
  }

  public static ArrayList<Point> simplifyPolyline(List<Point> polyLine, double tolerance) {
    ArrayList<Point> result = simplifyPolylineOneWay(polyLine, tolerance);
    Collections.reverse(result);
    result = simplifyPolylineOneWay(result, tolerance);
    Collections.reverse(result);
    return result;
  }

  public static double polylineLength(List<Point2D> polyline) {
    return polyline.stream().skip(1)
        .reduce(new Pair<Point2D, Double>(polyline.get(0), 0.0),
            (acc, point) -> new Pair<>(point, acc.getValue() + acc.getKey().distance(point)),
            (v0, v1) -> new Pair<>(v1.getKey(), v0.getValue() + v1.getValue()))
        .getValue();
  }

  public static java.awt.Point toAwtPoint(Point2D point2d) {
    return new java.awt.Point((int) Math.round(point2d.getX()), (int) Math.round(point2d.getY()));
  }

  public static ArrayList<java.awt.Point> rasterizeLine(Point2D l0, Point2D l1) {
    ArrayList<java.awt.Point> line = new ArrayList<>();
    int x0 = (int) Math.round(l0.getX());
    int x1 = (int) Math.round(l1.getX());
    int y0 = (int) Math.round(l0.getY());
    int y1 = (int) Math.round(l1.getY());

    int dx = Math.abs(x1 - x0);
    int sx = x0 < x1 ? 1 : -1;
    int dy = -Math.abs(y1 - y0);
    int sy = y0 < y1 ? 1 : -1;
    int err = dx + dy; /* error value e_xy */

    while (x0 != x1 || y0 != y1) {
      line.add(new java.awt.Point(x0, y0));
      int e2 = 2 * err;
      if (e2 >= dy) {
        err += dy;
        x0 += sx;
      }
      if (e2 <= dx) {
        err += dx;
        y0 += sy;
      }
    }
    line.add(new java.awt.Point(x1, y1));
    return line;
  }

  public static List<Point2D> toPolyline(Polygon polygon) {
    ArrayList<Point2D> points = new ArrayList<>();
    for (int i = 0; i < polygon.npoints; i++) {
      points.add(new Point2D.Double(polygon.xpoints[i], polygon.ypoints[i]));
    }
    return points;
  }

  public static List<Point2D> toPolyline(FloatPolygon polygon) {
    ArrayList<Point2D> points = new ArrayList<>();
    for (int i = 0; i < polygon.npoints; i++) {
      points.add(new Point2D.Double(polygon.xpoints[i], polygon.ypoints[i]));
    }
    return points;
  }

  /**
   * Returns any point of intersection if exists or null
   */
  public static Point2D intersectionPoint(Point2D a1, Point2D b1, Point2D a2, Point2D b2) {
    double x1 = a1.getX();
    double y1 = a1.getY();
    double x2 = b1.getX();
    double y2 = b1.getY();
    double x3 = a2.getX();
    double y3 = a2.getY();
    double x4 = b2.getX();
    double y4 = b2.getY();

    if (Line2D.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4)) {
      final double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
          / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
      final double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
          / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
      return new Point2D.Double(x, y);
    }
    return null;
  }

  public static List<Point2D>[] cutPolyline(List<Point2D> polyline, Point2D line1, Point2D line2) {
    for (int i = 1; i < polyline.size(); i++) {
      Point2D p1 = polyline.get(i - 1);
      Point2D p2 = polyline.get(i);
      Point2D ip = intersectionPoint(p1, p2, line1, line2);
      if (ip != null) {
        if (ip.equals(p1)) {
          if (i > 1) {
            List<Point2D> polyline1 = new ArrayList<>(polyline.subList(0, i));
            List<Point2D> polyline2 = new ArrayList<>(polyline.subList(i, polyline.size()));
            return new List[] {polyline1, polyline2};
          }
        } else if (ip.equals(p2)) {
          if (i < polyline.size() - 1) {
            List<Point2D> polyline1 = new ArrayList<>(polyline.subList(0, i + 1));
            List<Point2D> polyline2 = new ArrayList<>(polyline.subList(i + 1, polyline.size()));
            return new List[] {polyline1, polyline2};
          }
        } else {
          List<Point2D> polyline1 = new ArrayList<>(polyline.subList(0, i));
          polyline1.add(ip);
          List<Point2D> polyline2 = new ArrayList<>(polyline.subList(i, polyline.size()));
          polyline2.add(0, ip);
          return new List[] {polyline1, polyline2};
        }
        break;
      }
    }
    return new List[] {polyline};
  }

  public static List<Point2D> erasePolylineEnd(List<Point2D> oldPolyline, ShapeRoi shape,
      int beginOrEnd) {

    List<Point2D> newPolyline = new ArrayList<>(oldPolyline);
    if (beginOrEnd == BEGIN) {
      Collections.reverse(newPolyline);
    }

    int index;
    for (index = newPolyline.size() - 1; index >= 0 && shape
        .containsPoint(newPolyline.get(index).getX(), newPolyline.get(index).getY()); index--);

    if (index == newPolyline.size() - 1) {
      return newPolyline;
    }

    Point2D lastRemovedPoint = newPolyline.get(index + 1);
    newPolyline.subList(index + 1, newPolyline.size()).clear();

    if (index >= 0) {
      int ACCURACY = 2;
      Point2D newEnd = findSamplePointOnLine(lastRemovedPoint, newPolyline.get(index), ACCURACY,
          point -> !shape.containsPoint(point.getX(), point.getY()));
      if (newEnd != null && newEnd.distance(newPolyline.get(index)) > ACCURACY) {
        newPolyline.add(newEnd);
      }
    }

    if (beginOrEnd == BEGIN) {
      Collections.reverse(newPolyline);
    }
    return newPolyline;
  }

  public static Point2D findSamplePointOnLine(Point2D l1, Point2D l2, double step,
      Predicate<Point2D> predicate) {
    if (predicate.test(l1)) {
      return l1;
    }
    double dist = l1.distance(l2);
    int n = (int) Math.ceil(dist / step);
    // step vector
    double vx = (l2.getX() - l1.getX());
    double vy = (l2.getY() - l1.getY());
    for (int i = 1; i < n; i++) {
      Point2D sample = new Point2D.Double(l1.getX() + vx * (i / dist), l1.getY() + vy * (i / dist));
      if (predicate.test(sample)) {
        return sample;
      }
    }
    if (predicate.test(l2)) {
      return l2;
    }
    return null;
  }
}
