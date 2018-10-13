package dev.mtbt.cells.skeleton;

import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Point;
import sc.fiji.analyzeSkeleton.Vertex;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Utils {
  static boolean equalVertices (Vertex v1, Vertex v2) {
    List<Point> s1 = v1.getPoints();
    List<Point> s2 = v2.getPoints();
    if (s1.size() != s2.size())
      return false;
    for (Point p1 : s1) {
      boolean match = false;
      for (Point p2 : s2) {
        if (p1.x == p2.x && p1.y == p2.y) {
          match = true;
          break;
        }
      }
      if (!match)
        return false;
    }
    return true;
  }

  static boolean containsVertex (List<Vertex> vertices, Vertex v) {
    return findVertex(vertices, v) != null;
  }

  static boolean containsEdge (List<Edge> edges, Edge edge) {
    for (Edge e : edges) {
      boolean isEqual = equalVertices(e.getV1(), edge.getV1()) && equalVertices(e.getV2(), edge.getV2());
      isEqual = isEqual || equalVertices(e.getV1(), edge.getV2()) && equalVertices(e.getV2(), edge.getV1());
      if (isEqual)
        return true;
    }
    return false;
  }

  static Vertex findVertex (List<Vertex> vertices, Vertex vertex) {
    for (Vertex v : vertices) {
      if (equalVertices(v, vertex))
        return v;
    }
    return null;
  }

  static double distance (Point p0, Point p1) {
    return Math.sqrt(((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y)));
  }

  static double distance (Point point, ArrayList<Point> points) {
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
  static double perpendicularDistance (Point l1, Point l2, Point p) {
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
  public static ArrayList<Point> simplifyPolyLine (List<Point> polyLine, double tolerance) {
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
      results.addAll(simplifyPolyLine(polyLine.subList(0, maxIndex), tolerance));
      results.addAll(simplifyPolyLine(polyLine.subList(maxIndex, polyLine.size()), tolerance));
    } else if (polyLine.size() > 1) {
      results.add(polyLine.get(0));
      results.add(polyLine.get(polyLine.size() - 1));
    } else {
      results.add(polyLine.get(0));
    }
    return results;
  }

  static Point center (Vertex vertex) {
    ArrayList<Point> points = vertex.getPoints();
    if (points.size() == 1)
      return vertex.getPoints().get(0);
    long xSum = 0;
    long ySum = 0;
    for (Point point : points) {
      xSum += point.x;
      ySum += point.y;
    }
    int avgX = (int) Math.round((double) xSum / points.size());
    int avgY = (int) Math.round((double) ySum / points.size());
    return new Point(avgX, avgY, 0);
  }

  static ArrayList<Point> directedSlabs (Edge edge, Vertex begin) {
    ArrayList<Point> slabs = new ArrayList<>(edge.getSlabs());
    if (distance(center(begin), slabs.get(0)) > distance(center(begin), slabs.get(slabs.size() - 1))) {
      Collections.reverse(slabs);
    }
    return slabs;
  }
}
