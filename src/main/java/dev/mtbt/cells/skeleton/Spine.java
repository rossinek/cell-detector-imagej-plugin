package dev.mtbt.cells.skeleton;

import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Point;
import sc.fiji.analyzeSkeleton.Vertex;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dev.mtbt.cells.skeleton.Utils.*;

public class Spine {
  private ArrayList<Edge> edges = null;
  private ArrayList<Vertex> vertices = null;
  private Vertex endpoint1 = null;
  private Vertex endpoint1Origin = null;
  private Vertex endpoint2 = null;
  private Vertex endpoint2Origin = null;

  public Spine () {
    this.edges = new ArrayList<>();
    this.vertices = new ArrayList<>();
  }

  public ArrayList<Vertex> getVertices () {
    return vertices;
  }

  public Polygon toPolyLine () {
    ArrayList<Point> points = new ArrayList<>();
    if (endpoint1 != null) {
      points = toPath().toSlabs(true);
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
    return new Path(endpoint1, endpoint2, endpoint1.getBranches().get(0), endpoint2.getBranches().get(0));
  }

  public void traverse (Traverser t) {
    traverse(endpoint1, endpoint1.getBranches().get(0), t);
  }

  public void traverse (Vertex begin, Edge firstEdge, Traverser t) {
    Vertex current = begin;
    Edge nextEdge = firstEdge;
    while (true) {
      t.callback(current, nextEdge.getOppositeVertex(current), nextEdge);
      current = nextEdge.getOppositeVertex(current);
      if (isLeaf(current)) {
        break;
      }
      nextEdge = current.getBranches().get(current.getBranches().get(0) != nextEdge ? 0 : 1);
    }
  }

  public Edge addEdge (Edge e) {
    validateNewEdge(e);
    Vertex v1 = this.addVertex(e.getV1());
    Vertex v2 = this.addVertex(e.getV2());
    Edge edge = e.clone(v1, v2);
    if (edges.size() == 0) {
      endpoint1 = v1;
      endpoint1Origin = e.getV1();
      endpoint2 = v2;
      endpoint2Origin = e.getV2();
    } else if (v1 == endpoint1) {
      endpoint1 = v2;
      endpoint1Origin = e.getV2();
    } else if (v1 == endpoint2) {
      endpoint2 = v2;
      endpoint2Origin = e.getV2();
    } else if (v2 == endpoint1) {
      endpoint1 = v1;
      endpoint1Origin = e.getV1();
    } else if (v2 == endpoint2) {
      endpoint2 = v1;
      endpoint2Origin = e.getV1();
    }
    v1.setBranch(edge);
    v2.setBranch(edge);
    edges.add(edge);
    return edge;
  }

  public boolean overlaps (Spine spine) {
    return commonEdges(spine.edges) > 0;
  }

  /**
   * Split Spine in the weakest point between points
   * @return array containing two spines: first containing p1, second containing p2
   */
  public Spine[] split (Point p1, Point p2, PointEvaluator pointEvaluator) {
    Edge e1 = closestEdge(edges, p1);
    Edge e2 = closestEdge(edges, p2);
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
    System.out.println("Weakest point: " + weakestPoint.toString());
    Spine[] spines = split(weakestPoint);
    if (index1 > index2) Collections.reverse(Arrays.asList(spines));
    return spines;
  }

  private Spine[] split (Point slab) {
    Edge edge = findEdge(slab);
    Edge[] newEdges = Utils.split(edge, slab);
    Spine[] spines = new Spine[2];
    for (int i = 0; i < newEdges.length; i++) {
      Edge branch = newEdges[i];
      Spine spine = new Spine();
      spines[i] = spine;
      traverse(branch.getV1(), branch, (v1, v2, e) -> spine.addEdge(e));
    }
    return spines;
  }

  public interface EdgeEvaluator {
    double score (Edge edge, Vertex start);
  }

  /**
   * Extend spine based on original graph
   */
  public void extend (EdgeEvaluator edgeEvaluator) {
    while (extend(endpoint1, edgeEvaluator));
    while (extend(endpoint2, edgeEvaluator));
  }

  private Edge strongestValidEdge (ArrayList<Edge> candidates, Vertex start, EdgeEvaluator edgeEvaluator) {
    double bestScore = Double.NEGATIVE_INFINITY;
    Edge bestEdge = null;
    for (Edge candidate : candidates) {
      // if valid edge
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

  private boolean extend (Vertex endpoint, EdgeEvaluator edgeEvaluator) {
    Vertex endpointOrigin;
    if (endpoint == endpoint1) endpointOrigin = endpoint1Origin;
    else if (endpoint == endpoint2) endpointOrigin = endpoint2Origin;
    else throw new IllegalArgumentException("Vertex is not spline endpoint");
    if (isLeaf(endpointOrigin)) return false;
    Edge newEdge = strongestValidEdge(endpointOrigin.getBranches(), endpoint, edgeEvaluator);
    if (newEdge == null) return false;
    addEdge(newEdge);
    return true;
  }

  private void validateNewEdge (Edge e) {
    if (edges.size() == 0)
      return;
    if (commonVertices(e) > 1) {
      throw new IllegalArgumentException("Edge is creating cycle in Spine");
    }
    boolean endpointConnection;
    endpointConnection = equalVertices(e.getV1(), endpoint1);
    endpointConnection = endpointConnection || equalVertices(e.getV1(), endpoint2);
    endpointConnection = endpointConnection || equalVertices(e.getV2(), endpoint1);
    endpointConnection = endpointConnection || equalVertices(e.getV2(), endpoint2);
    if (!endpointConnection) {
      throw new IllegalArgumentException("Edge is not connected with Spine endpoints");
    }
  }

  private Vertex addVertex (Vertex v) {
    Vertex vertex = findVertex(v);
    if (vertex == null) {
      vertex = v.cloneUnconnected();
      vertices.add(vertex);
    }
    return vertex;
  }

  private Vertex findVertex (Vertex vertex) {
    return Utils.findVertex(this.vertices, vertex);
  }

  private boolean containsVertex (Vertex vertex) {
    return Utils.containsVertex(this.vertices, vertex);
  }

  private int commonVertices (Edge edge) {
    return commonVertices(new Vertex[] {edge.getV1(), edge.getV2()});
  }

  private int commonVertices (Vertex[] vertices) {
    int count = 0;
    for (Vertex vertex : vertices) {
      if (containsVertex(vertex))
        count++;
    }
    return count;
  }

  private int commonEdges (List<Edge> edges) {
    int count = 0;
    for (Edge edge : edges) {
      if (containsEdge(this.edges, edge))
        count++;
    }
    return count;
  }

  private boolean isLeaf (Vertex v) {
    return v.getBranches().size() == 1;
  }

  private Edge findEdge (Point slab) {
    for (Edge edge : edges) {
      for (Point p : edge.getSlabs()) {
        if (p.x == slab.x && p.y == slab.y) return edge;
      }
    }
    return null;
  }

  private Path findPath (Edge e1, Edge e2) {
    Path path = findPath(e1.getV1(), e2.getV1());
    if (path == null) path = findPath(e1.getV2(), e2.getV1());
    if (path == null) return null;
    if (path.getFirstEdge() != e1) path.extendBegin();
    if (path.getLastEdge() != e2) path.extendEnd();
    return path;
  }


  private Path findPath (Vertex v1, Vertex v2) {
    for (Edge branch : v1.getBranches()) {
      ArrayList<Edge> path = new ArrayList<>();
      Vertex current = v1;
      Edge nextEdge = branch;
      while (true) {
        path.add(nextEdge);
        current = nextEdge.getOppositeVertex(current);
        if (current == v2) return new Path(v1, v2, branch, nextEdge);
        if (isLeaf(current)) break;
        nextEdge = current.getBranches().get(current.getBranches().get(0) != nextEdge ? 0 : 1);
      }
    }
    return null;
  }

  public interface PointEvaluator {
    double score (Point p);
  }

  public interface Traverser {
    void callback (Vertex v1, Vertex v2, Edge e);
  }

  class Path {
    private Vertex begin;
    private Vertex end;
    private Edge firstEdge;
    private Edge lastEdge;

    public Path (Vertex begin, Vertex end, Edge firstEdge, Edge lastEdge) {
      this.begin = begin;
      this.end = end;
      this.firstEdge = firstEdge;
      this.lastEdge = lastEdge;
    }

    public Vertex getBegin () {
      return begin;
    }

    public Vertex getEnd () {
      return end;
    }

    public Edge getFirstEdge () {
      return firstEdge;
    }

    public Edge getLastEdge () {
      return lastEdge;
    }

    public void extendBegin () {
      firstEdge = begin.getBranches().get(begin.getBranches().get(0) == firstEdge ? 1 : 0);
      begin = firstEdge.getOppositeVertex(begin);
    }

    public void extendEnd () {
      lastEdge = end.getBranches().get(end.getBranches().get(0) == lastEdge ? 1 : 0);
      end = lastEdge.getOppositeVertex(end);
    }

    public ArrayList<Point> toSlabs (boolean addEndpoints) {
      ArrayList<Point> points = new ArrayList<>();

      if (addEndpoints) points.add(center(begin));
      traverse((v1, v2, e) -> {
        points.addAll(directedSlabs(e, v1));
      });
      if (addEndpoints) points.add(center(end));
      return points;
    }

    public void traverse (Traverser t) {
      Vertex current = begin;
      Edge nextEdge = firstEdge;
      while (true) {
        t.callback(current, nextEdge.getOppositeVertex(current), nextEdge);
        current = nextEdge.getOppositeVertex(current);
        if (current == end) {
          break;
        }
        nextEdge = current.getBranches().get(current.getBranches().get(0) != nextEdge ? 0 : 1);
      }
    }
  }
}
