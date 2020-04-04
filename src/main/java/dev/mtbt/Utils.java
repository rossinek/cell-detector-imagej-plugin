package dev.mtbt;

import dev.mtbt.graph.*;
import dev.mtbt.util.Pair;
import java.util.*;
import java.awt.geom.Point2D;

public class Utils {
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
    return simplifyPolylineOneWay(result, tolerance);
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
}
