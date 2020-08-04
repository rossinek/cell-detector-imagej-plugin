package dev.mtbt.cells.skeleton;

import dev.mtbt.graph.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;
import dev.mtbt.util.Geometry;
import dev.mtbt.cells.skeleton.SpineConflictsResolver.OverlapType;
import dev.mtbt.cells.skeleton.SpineTraverser.SpineTraverserStep;

public class Spine extends Graph {

  private SpineVertex e1 = null;
  private SpineVertex e2 = null;

  public Spine() {
    super();
  }

  public SpineVertex getE1() {
    return this.e1;
  }

  public SpineVertex getE2() {
    return this.e2;
  }

  public List<Point2D> toPolyline() {
    ArrayList<Point> points = new ArrayList<>();
    if (this.e1 != null) {
      points = this.toPath().toSlabs(true);
      points = Geometry.simplifyPolyline(points, 2);
    }
    return points.stream().map(p -> p.toPoint2D()).collect(Collectors.toCollection(ArrayList::new));
  }

  public void reverse() {
    SpineVertex temp = this.e1;
    this.e1 = this.e2;
    this.e2 = temp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!Spine.class.isAssignableFrom(o.getClass()))
      return false;
    final Spine s = (Spine) o;
    return SpineConflictsResolver.overlapType(this, s) == OverlapType.Full;
  }

  @Override
  public Edge addEdge(Edge e) {
    validateNewEdge(e);
    SpineVertex v1 = (SpineVertex) this.addVertex(new SpineVertex(e.getV1()));
    SpineVertex v2 = (SpineVertex) this.addVertex(new SpineVertex(e.getV2()));
    Edge edge = e.clone(v1, v2);
    if (this.edges.size() == 0) {
      this.e1 = v1;
      this.e2 = v2;
    } else if (edge.isIncidentTo(this.e1)) {
      this.e1 = (SpineVertex) edge.getOppositeVertex(this.e1);
    } else if (edge.isIncidentTo(this.e2)) {
      this.e2 = (SpineVertex) edge.getOppositeVertex(this.e2);
    }
    return super.addEdge(edge);
  }

  /**
   * Extend spine based on original graph
   */
  public void extend(IEdgeEvaluator edgeEvaluator) {
    while (extend(e1, edgeEvaluator));
    while (extend(e2, edgeEvaluator));
  }

  private Edge strongestValidEdge(Set<Edge> candidates, Vertex start,
      IEdgeEvaluator edgeEvaluator) {
    double bestScore = Double.NEGATIVE_INFINITY;
    Edge bestEdge = null;
    for (Edge candidate : candidates) {
      if (commonVertices(candidate) == 1) {
        double score = edgeEvaluator.score(candidate, start);
        if (score > bestScore) {
          bestScore = score;
          bestEdge = candidate;
        }
      }
    }
    return bestEdge;
  }

  public boolean extend(Vertex endpoint, IEdgeEvaluator edgeEvaluator) {
    Vertex endpointOrigin;
    if (this.e1.equals(endpoint)) {
      endpointOrigin = this.e1.getSkeletonVertex();
    } else if (this.e2.equals(endpoint)) {
      endpointOrigin = this.e2.getSkeletonVertex();
    } else {
      throw new IllegalArgumentException("Vertex is not spine endpoint");
    }
    if (endpointOrigin.isLeaf()) {
      return false;
    }
    Edge newEdge = strongestValidEdge(endpointOrigin.getBranches(), endpoint, edgeEvaluator);
    if (newEdge == null) {
      return false;
    }
    return addEdge(newEdge) != null;
  }

  private void validateNewEdge(Edge e) {
    if (this.edges.size() == 0) {
      return;
    }
    if (commonVertices(e) > 1) {
      throw new IllegalArgumentException("Edge is creating cycle in Spine");
    }
    if (!e.getV1().equals(this.e1) && !e.getV1().equals(this.e2) && !e.getV2().equals(this.e1)
        && !e.getV2().equals(this.e2)) {
      throw new IllegalArgumentException("Edge is not connected with Spine endpoints");
    }
  }

  private int commonVertices(Edge edge) {
    return this.commonVertices(new Vertex[] {edge.getV1(), edge.getV2()});
  }

  private int commonVertices(Vertex[] vertices) {
    int count = 0;
    for (Vertex vertex : vertices) {
      if (this.vertices.contains(vertex)) {
        count++;
      }
    }
    return count;
  }

  protected Edge getAnyCommonEdge(Edge[] edges) {
    for (Edge edge : edges) {
      if (this.edges.contains(edge)) {
        return edge;
      }
    }
    return null;
  }

  protected Edge findEdge(Point slab) {
    for (Edge edge : edges) {
      for (Point p : edge.getSlabs()) {
        if (p.equals(slab)) {
          return edge;
        }
      }
    }
    return null;
  }

  public java.awt.geom.Point2D.Double getBegin() {
    if (this.e1 == null) {
      return null;
    }
    return this.e1.getSkeletonVertex().center().toPoint2D();
  }

  public java.awt.geom.Point2D.Double getEnd() {
    if (this.e2 == null) {
      return null;
    }
    return this.e2.getSkeletonVertex().center().toPoint2D();
  }

  public void assign(Spine s) {
    super.assign(s);
    this.e1 = s.e1;
    this.e2 = s.e2;
  }

  public boolean overlaps(Spine spine) {
    return SpineConflictsResolver.overlapType(this, spine) != OverlapType.None;
  }

  protected SpineVertex shorten(SpineVertex endpoint) {
    if (this.edges.size() < 1) {
      throw new IllegalArgumentException("No edges!");
    }
    if (!endpoint.equals(this.e1) && !endpoint.equals(this.e2)) {
      throw new IllegalArgumentException("Vertex is not spine endpoint");
    }
    if (this.edges.size() == 1) {
      this.edges.clear();
      this.vertices.clear();
      this.e1 = null;
      this.e2 = null;
      return null;
    }
    Edge edge = endpoint.getBranches().first();
    SpineVertex newEndpoint = (SpineVertex) edge.getOppositeVertex(endpoint);
    newEndpoint.getBranches().remove(edge);
    this.edges.remove(edge);
    this.vertices.remove(endpoint);
    if (endpoint.equals(this.e1)) {
      this.e1 = newEndpoint;
    } else {
      this.e2 = newEndpoint;
    }
    return newEndpoint;
  }

  protected Path toPath() {
    return new Path(this.e1, this.e2, this.e1.getBranches().first(), this.e2.getBranches().first());
  }

  protected Path findPath(Edge e1, Edge e2) {
    if (e1.equals(e2)) {
      return new Path((SpineVertex) e1.getV1(), (SpineVertex) e1.getV2(), e1, e2);
    }
    Path path;
    if (e1.getV1().equals(e2.getV2())) {
      path = findPath((SpineVertex) e1.getV2(), (SpineVertex) e2.getV1());
    } else {
      path = findPath((SpineVertex) e1.getV1(), (SpineVertex) e2.getV2());
    }

    if (path == null)
      return null;
    if (!path.getFirstEdge().equals(e1))
      path.extendBegin();
    if (!path.getLastEdge().equals(e2))
      path.extendEnd();
    return path;
  }

  protected Path findPath(SpineVertex v1, SpineVertex v2) {
    for (Edge branch : v1.getBranches()) {
      SpineVertex current = v1;
      Edge nextEdge = branch;
      while (true) {
        current = (SpineVertex) nextEdge.getOppositeVertex(current);
        if (current.equals(v2)) {
          return new Path(v1, v2, branch, nextEdge);
        }
        if (current.isLeaf())
          break;
        nextEdge = current.getOppositeBranch(nextEdge);
      }
    }
    return null;
  }

  static class Path {
    private SpineVertex begin;
    private SpineVertex end;
    private Edge firstEdge;
    private Edge lastEdge;

    public Path(SpineVertex begin, SpineVertex end, Edge firstEdge, Edge lastEdge) {
      this.begin = begin;
      this.end = end;
      this.firstEdge = firstEdge;
      this.lastEdge = lastEdge;
    }

    public SpineVertex getBegin() {
      return begin;
    }

    public SpineVertex getEnd() {
      return end;
    }

    public Edge getFirstEdge() {
      return firstEdge;
    }

    public Edge getLastEdge() {
      return lastEdge;
    }

    public void extendBegin() {
      if (!begin.isLeaf()) {
        firstEdge = begin.getOppositeBranch(firstEdge);
        begin = (SpineVertex) firstEdge.getOppositeVertex(begin);
      }
    }

    public void extendEnd() {
      if (!end.isLeaf()) {
        lastEdge = end.getOppositeBranch(lastEdge);
        end = (SpineVertex) lastEdge.getOppositeVertex(end);
      }
    }

    public ArrayList<Point> toSlabs(boolean addEndpoints) {
      ArrayList<Point> points = new ArrayList<>();

      if (addEndpoints)
        points.add(begin.center());

      SpineTraverser traverser = new SpineTraverser(begin, firstEdge, end);
      while (traverser.hasNext()) {
        SpineTraverserStep step = traverser.next();
        points.addAll(step.edge.getDirectedSlabs(step.v1));
      }
      if (addEndpoints)
        points.add(end.center());
      return points;
    }
  }
}
