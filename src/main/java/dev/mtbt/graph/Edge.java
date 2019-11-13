package dev.mtbt.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import static dev.mtbt.Utils.distance;
import static java.util.stream.Collectors.toList;

public class Edge implements Comparable<Edge> {
  protected Vertex v1;
  protected Vertex v2;
  protected ArrayList<Point> slabs;

  public Edge(Vertex v1, Vertex v2, ArrayList<Point> slabs) {
    if (v1.compareTo(v2) <= 0) {
      this.v1 = v1;
      this.v2 = v2;
    } else {
      this.v1 = v2;
      this.v2 = v1;
    }
    this.slabs = slabs == null ? new ArrayList<>() : slabs;
    this.fixSlabsDirection();
  }

  public Edge(sc.fiji.analyzeSkeleton.Edge e, Vertex v1, Vertex v2) {
    this(v1, v2, null);
    if (e.getSlabs() != null) {
      this.slabs.addAll(e.getSlabs().stream().map(Point::new).collect(toList()));
      this.fixSlabsDirection();
    }
  }

  private void fixSlabsDirection() {
    if (this.slabs.isEmpty())
      return;
    if (distance(this.v1.center(), slabs.get(0)) > distance(this.v1.center(),
        slabs.get(slabs.size() - 1))) {
      Collections.reverse(slabs);
    }
  }

  public Vertex getV1() {
    return this.v1;
  }

  public Vertex getV2() {
    return this.v2;
  }

  public ArrayList<Point> getSlabs() {
    return this.slabs;
  }

  public ArrayList<Point> getDirectedSlabs(Vertex begin) {
    if (!begin.equals(this.v1) && !begin.equals(this.v2))
      throw new IllegalArgumentException("Begin is not edge vertex");
    ArrayList<Point> slabs = new ArrayList<>(this.slabs);
    if (begin.equals(this.v2))
      Collections.reverse(slabs);
    return slabs;
  }

  public Vertex getOppositeVertex(Vertex v) {
    if (this.v1.equals(v))
      return this.v2;
    else if (this.v2.equals(v))
      return this.v1;
    return null;
  }

  public Edge clone(final Vertex v1, final Vertex v2) {
    ArrayList<Point> clonedSlabs = null;
    if (slabs != null) {
      clonedSlabs =
          slabs.stream().map(Point::clone).collect(Collectors.toCollection(ArrayList::new));
    }
    return new Edge(v1, v2, clonedSlabs);
  }

  public boolean isIncidentTo(Vertex v) {
    return v.equals(this.v1) || v.equals(this.v2);
  }

  @Override
  public String toString() {
    return v1.toString() + " <--> " + v2.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!Edge.class.isAssignableFrom(o.getClass()))
      return false;
    final Edge e = (Edge) o;
    return (v1.equals(e.v1) && v2.equals(e.v2));
  }

  @Override
  public int compareTo(Edge e) {
    int diff = v1.compareTo(e.getV1());
    if (diff != 0)
      return diff;
    diff = v2.compareTo(e.getV2());
    return diff;
  }
}
