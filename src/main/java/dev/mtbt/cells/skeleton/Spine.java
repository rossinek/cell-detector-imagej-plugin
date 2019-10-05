package dev.mtbt.cells.skeleton;

import dev.mtbt.graph.*;

import java.awt.Polygon;
import java.util.*;
import java.util.stream.Collectors;

import static dev.mtbt.Utils.*;

public class Spine extends Graph {

  private SpineVertex e1 = null;
  private SpineVertex e2 = null;

  public Spine () {
    super();
  }

  public Polygon toPolyLine () {
    ArrayList<Point> points = new ArrayList<>();
    if (this.e1 != null) {
      points = this.toPath().toSlabs(true);
      points = simplifyPolyLine(points, 2);
    }
    int xpoints[] = new int[points.size()];
    int ypoints[] = new int[points.size()];
    for (int i = 0; i < points.size(); i++) {
      xpoints[i] = points.get(i).x;
      ypoints[i] = points.get(i).y;
    }
    return new Polygon(xpoints, ypoints, points.size());
  }

  private Path toPath () {
    return new Path(this.e1, this.e2, this.e1.getBranches().first(), this.e2.getBranches().first());
  }

  public void traverse (GraphTraverser t) {
    traverse(this.e1, this.e1.getBranches().first(), t);
  }

  public void traverse (SpineVertex begin, Edge firstEdge, GraphTraverser t) {
    SpineVertex current = begin;
    Edge nextEdge = firstEdge;
    while (true) {
      t.callback(current, nextEdge.getOppositeVertex(current), nextEdge);
      current = (SpineVertex) nextEdge.getOppositeVertex(current);
      if (current.isLeaf())
        break;
      nextEdge = current.getOppositeBranch(nextEdge);
    }
  }

  @Override
  public Edge addEdge (Edge e) {
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

  public boolean overlaps (Spine spine) {
    return commonEdges(spine.edges) > 0;
  }

  /**
   * Split Spine in the weakest point between points
   * @return array containing two spines: first containing p1, second containing p2
   */
  public Spine[] split (Point p1, Point p2, PointEvaluator pointEvaluator) {
    Edge e1 = this.closestEdge(p1);
    Edge e2 = this.closestEdge(p2);
    ArrayList<Point> points = findPath(e1, e2).toSlabs(false);
    double minDist1 = Double.POSITIVE_INFINITY;
    double minDist2 = Double.POSITIVE_INFINITY;
    int index1 = -1, index2 = -1;
    double dist;
    for (int i = 0; i < points.size(); i++) {
      dist = distance(p1, points.get(i));
      if (dist < minDist1) {
        minDist1 = dist;
        index1 = i;
      }
      dist = distance(p2, points.get(i));
      if (dist < minDist2) {
        minDist2 = dist;
        index2 = i;
      }
    }
    int bi = Math.min(index1, index2);
    int ei = Math.max(index1, index2);
    Point weakestPoint = null;
    double minScore = Double.POSITIVE_INFINITY;
    for (int i = bi + 1; i < ei; i++) {
      double score = pointEvaluator.score(points.get(i));
      if (score < minScore) {
        minScore = score;
        weakestPoint = points.get(i);
      }
    }
    Spine[] spines = split(weakestPoint);
    if (index1 > index2) Collections.reverse(Arrays.asList(spines));
    return spines;
  }

  private Spine[] split (Point slab) {
    Edge edge = findEdge(slab);
    Edge[] newEdges = this.splitEdge(edge, slab);
    Spine[] spines = new Spine[2];
    for (int i = 0; i < newEdges.length; i++) {
      Edge newBranch = newEdges[i];
      Spine spine = new Spine();
      spines[i] = spine;
      spine.addEdge(newBranch);
      SpineVertex start = (SpineVertex) (newBranch.isIncidentTo(edge.getV1()) ? edge.getV1() : edge.getV2());
      if (!start.isLeaf()) {
        Edge firstEdge = start.getOppositeBranch(edge);
        traverse(start, firstEdge, (v1, v2, e) -> spine.addEdge(e));
      }
    }
    return spines;
  }

  private Edge[] splitEdge (Edge e, Point slab) {
    int index = e.getSlabs().indexOf(slab);
    ArrayList<Point> slabs1 = new ArrayList<>();
    ArrayList<Point> slabs2 = new ArrayList<>();
    if (index >= 0) {
      slabs1.addAll(e.getSlabs().subList(0, index));
      slabs2.addAll(e.getSlabs().subList(index+1, e.getSlabs().size()));
    }
    Vertex vSlab1 = new SpineVertex();
    vSlab1.addPoint(slab);
    Vertex v1 = e.getV1().cloneUnconnected();
    Vertex vSlab2 = vSlab1.cloneUnconnected();
    Vertex v2 = e.getV2().cloneUnconnected();
    Edge edge1 = new Edge(vSlab1, v1, slabs1);
    Edge edge2 = new Edge(vSlab2, v2, slabs2);
    return new Edge[] { edge1, edge2 };
  }

  /**
   * Extend spine based on original graph
   */
  public void extend (EdgeEvaluator edgeEvaluator) {
    while (extend(e1, edgeEvaluator));
    while (extend(e2, edgeEvaluator));
  }

  private Edge strongestValidEdge (Set<Edge> candidates, Vertex start, EdgeEvaluator edgeEvaluator) {
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

  public boolean extend (Vertex endpoint, EdgeEvaluator edgeEvaluator) {
    Vertex endpointOrigin;
    if (this.e1.equals(endpoint)) endpointOrigin = this.e1.getSkeletonVertex();
    else if (this.e2.equals(endpoint)) endpointOrigin = this.e2.getSkeletonVertex();
    else throw new IllegalArgumentException("Vertex is not spline endpoint");
    if (endpointOrigin.isLeaf()) return false;
    Edge newEdge = strongestValidEdge(endpointOrigin.getBranches(), endpoint, edgeEvaluator);
    if (newEdge == null) return false;
    return addEdge(newEdge) != null;
  }

  private void validateNewEdge (Edge e) {
    if (this.edges.size() == 0) return;
    if (commonVertices(e) > 1) {
      throw new IllegalArgumentException("Edge is creating cycle in Spine");
    }
    if (!e.getV1().equals(this.e1) && !e.getV1().equals(this.e2) && !e.getV2().equals(this.e1) && !e.getV2().equals(this.e2)) {
      throw new IllegalArgumentException("Edge is not connected with Spine endpoints");
    }
  }

  private int commonVertices (Edge edge) {
    return commonVertices(Arrays.stream(new Vertex[] {edge.getV1(), edge.getV2()}).collect(Collectors.toSet()));
  }

  private int commonVertices (Set<Vertex> vertices) {
    int count = 0;
    for (Vertex vertex : vertices) {
      if (this.vertices.contains(vertex)) count++;
    }
    return count;
  }

  private int commonEdges (Set<Edge> edges) {
    int count = 0;
    for (Edge edge : edges) {
      if (this.edges.contains(edge)) count++;
    }
    return count;
  }

  private Edge findEdge (Point slab) {
    for (Edge edge : edges) {
      for (Point p : edge.getSlabs()) {
        if (p.equals(slab)) return edge;
      }
    }
    return null;
  }

  private Path findPath (Edge e1, Edge e2) {
    if (e1.equals(e2)) {
      return new Path((SpineVertex) e1.getV1(), (SpineVertex) e1.getV2(), e1, e2);
    }
    Path path;
    if (e1.getV1().equals(e2.getV2())) {
      path = findPath((SpineVertex) e1.getV2(), (SpineVertex) e2.getV1());
    } else {
      path = findPath((SpineVertex) e1.getV1(), (SpineVertex) e2.getV2());
    }

    if (path == null) return null;
    if (!path.getFirstEdge().equals(e1)) path.extendBegin();
    if (!path.getLastEdge().equals(e2)) path.extendEnd();
    return path;
  }

  private Path findPath (SpineVertex v1, SpineVertex v2) {
    for (Edge branch : v1.getBranches()) {
      SpineVertex current = v1;
      Edge nextEdge = branch;
      while (true) {
        current = (SpineVertex) nextEdge.getOppositeVertex(current);
        if (current.equals(v2)) {
          return new Path(v1, v2, branch, nextEdge);
        }
        if (current.isLeaf()) break;
        nextEdge = current.getOppositeBranch(nextEdge);
      }
    }
    return null;
  }

  class Path {
    private SpineVertex begin;
    private SpineVertex end;
    private Edge firstEdge;
    private Edge lastEdge;

    public Path (SpineVertex begin, SpineVertex end, Edge firstEdge, Edge lastEdge) {
      this.begin = begin;
      this.end = end;
      this.firstEdge = firstEdge;
      this.lastEdge = lastEdge;
    }

    public SpineVertex getBegin () {
      return begin;
    }

    public SpineVertex getEnd () {
      return end;
    }

    public Edge getFirstEdge () {
      return firstEdge;
    }

    public Edge getLastEdge () {
      return lastEdge;
    }

    public void extendBegin () {
      if (!begin.isLeaf()) {
        firstEdge = begin.getOppositeBranch(firstEdge);
        begin = (SpineVertex) firstEdge.getOppositeVertex(begin);
      }
    }

    public void extendEnd () {
      if (!end.isLeaf()) {
        lastEdge = end.getOppositeBranch(lastEdge);
        end = (SpineVertex) lastEdge.getOppositeVertex(end);
      }
    }

    public ArrayList<Point> toSlabs (boolean addEndpoints) {
      ArrayList<Point> points = new ArrayList<>();

      if (addEndpoints) points.add(begin.center());
      traverse((v1, v2, e) -> points.addAll(e.getDirectedSlabs(v1)));
      if (addEndpoints) points.add(end.center());
      return points;
    }

    public void traverse (GraphTraverser t) {
      SpineVertex current = begin;
      Edge nextEdge = firstEdge;
      while (true) {
        t.callback(current, nextEdge.getOppositeVertex(current), nextEdge);
        current = (SpineVertex) nextEdge.getOppositeVertex(current);
        if (current.equals(end)) break;
        nextEdge = current.getOppositeBranch(nextEdge);
      }
    }
  }
}
