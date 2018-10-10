package dev.mtbt.graph;

import java.util.Iterator;
import java.util.TreeSet;
import static java.util.stream.Collectors.toList;

public class Vertex implements Comparable<Vertex> {
  protected TreeSet<Point> points;
  protected TreeSet<Edge> branches;

  public Vertex () {
    this.points = new TreeSet<>();
    this.branches = new TreeSet<>();
  }

  public Vertex (sc.fiji.analyzeSkeleton.Vertex v) {
    this.points = new TreeSet<>();
    this.branches = new TreeSet<>();
    points.addAll(v.getPoints().stream().map(Point::new).collect(toList()));
  }

  public void addPoint (Point p) {
    this.points.add(p);
  }

  public boolean isVertexPoint (Point p) {
    return points != null && points.contains(p);
  }

  public TreeSet<Point> getPoints () {
    return this.points;
  }

  public void setBranch (Edge e) {
    this.branches.add(e);
  }

  public TreeSet<Edge> getBranches () {
    return this.branches;
  }

  public boolean isLeaf () {
    return branches.size() < 2;
  }

  public Point center () {
    if (points.size() == 1) return points.first();
    long xSum = 0;
    long ySum = 0;
    long zSum = 0;
    for (Point point : points) {
      xSum += point.x;
      ySum += point.y;
    }
    int avgX = (int) Math.round((double) xSum / points.size());
    int avgY = (int) Math.round((double) ySum / points.size());
    int avgZ = (int) Math.round((double) zSum / points.size());
    return new Point(avgX, avgY, avgZ);
  }

  public Vertex cloneUnconnected () {
    final Vertex clone = new Vertex();
    clone.points.addAll(points.stream().map(Point::clone).collect(toList()));
    return clone;
  }

  @Override
  public String toString () {
    StringBuilder sb = new StringBuilder();
    sb.append("< ");
    for(final Point p : this.points)
      sb.append(p.toString()).append(" ");
    sb.append(">");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!Vertex.class.isAssignableFrom(o.getClass())) return false;
    final Vertex v = (Vertex) o;
    return points.equals(v.points);
  }

  @Override
  public int compareTo (Vertex p) {
    Iterator<Point> iter1  = points.iterator();
    Iterator<Point> iter2  = p.getPoints().iterator();
    while (iter1.hasNext() && iter2.hasNext()) {
      Point p1 = iter1.next();
      Point p2 = iter2.next();
      int diff = p1.compareTo(p2);
      if (diff != 0) return diff;
    }
    return points.size() - p.getPoints().size();
  }
}
