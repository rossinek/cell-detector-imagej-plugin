package dev.mtbt.graph;

public class Point implements Comparable<Point> {
  public int x;
  public int y;

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Point(int x, int y, int z) {
    this.x = x;
    this.y = y;
  }

  public Point(sc.fiji.analyzeSkeleton.Point p) {
    this.x = p.x;
    this.y = p.y;
  }

  public Point(java.awt.Point p) {
    this(p.x, p.y);
  }

  @Override
  public String toString () {
    return "(" + this.x + ", " + this.y + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Point p = (Point) o;
    return p.x == this.x && p.y == this.y;
  }

  @Override
  public Point clone () {
    return new Point(this.x, this.y);
  }

  @Override
  public int compareTo (Point p) {
    int diff = this.x - p.x;
    if (diff != 0) return diff;
    diff = this.y - p.y;
    return diff;
  }
}
