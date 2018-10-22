package dev.mtbt;

import dev.mtbt.graph.*;

import java.util.*;

public class Utils {
  public static double distance (Point p0, Point p1) {
    return Math.sqrt(((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y)));
  }

  public static double distance (Point point, Set<Point> points) {
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
  public static double perpendicularDistance (Point l1, Point l2, Point p) {
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
  public static ArrayList<Point> simplifyPolyLineOneWay (List<Point> polyLine, double tolerance) {
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
      results.addAll(simplifyPolyLineOneWay(polyLine.subList(0, maxIndex), tolerance));
      results.addAll(simplifyPolyLineOneWay(polyLine.subList(maxIndex, polyLine.size()), tolerance));
    } else if (polyLine.size() > 1) {
      results.add(polyLine.get(0));
      results.add(polyLine.get(polyLine.size() - 1));
    } else {
      results.add(polyLine.get(0));
    }
    return results;
  }

  public static ArrayList<Point> simplifyPolyLine (List<Point> polyLine, double tolerance) {
    ArrayList<Point> result = simplifyPolyLineOneWay(polyLine, tolerance);
    Collections.reverse(result);
    return simplifyPolyLineOneWay(result, tolerance);
  }
}
