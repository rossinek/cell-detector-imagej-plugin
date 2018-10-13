package dev.mtbt.cells.skeleton;

import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.analyzeSkeleton.Graph;
import sc.fiji.analyzeSkeleton.Point;
import sc.fiji.analyzeSkeleton.Vertex;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dev.mtbt.cells.skeleton.Utils.*;

public class Spine {
  Graph[] graphs;
  private ArrayList<Edge> edges = null;
  private ArrayList<Vertex> vertices = null;
  private Vertex endpoint1 = null;
  private Vertex endpoint1Origin = null;
  private Vertex endpoint2 = null;
  private Vertex endpoint2Origin = null;

  public Spine (Graph[] graphs) {
    this.edges = new ArrayList<>();
    this.vertices = new ArrayList<>();
    this.graphs = graphs;
  }

  public ArrayList<Vertex> getVertices () {
    return vertices;
  }

  public Polygon toPolyLine () {
    ArrayList<Point> points = new ArrayList<>();
    if (endpoint1 != null) {
      Vertex current = endpoint1;
      Edge nextEdge = endpoint1.getBranches().get(0);
      points.add(center(endpoint1));
      while (true) {
        points.addAll(directedSlabs(nextEdge, current));
        current = nextEdge.getOppositeVertex(current);
        if (current == endpoint2) {
          points.add(center(current));
          break;
        }
        if (current.getBranches().get(0) != nextEdge)
          nextEdge = current.getBranches().get(0);
        else
          nextEdge = current.getBranches().get(1);
      }
    }
    points = simplifyPolyLine(points, 2);
    int xpoints[] = new int[points.size()];
    int ypoints[] = new int[points.size()];
    for (int i = 0; i < points.size(); i++) {
      xpoints[i] = points.get(i).x;
      ypoints[i] = points.get(i).y;
    }
    return new Polygon(xpoints, ypoints, points.size());
  }

  public interface EdgeEvaluator {
    double score (Edge edge, Vertex start);
  }

  /**
   * Extend spine based on original graph
   */
  public void extend (EdgeEvaluator edgeEvaluator) {
    // TODO: find spine
    System.out.println("> Extend");
    while (extend(endpoint1, edgeEvaluator))
      ;
    while (extend(endpoint2, edgeEvaluator))
      ;
  }

  private boolean extend (Vertex endpoint, EdgeEvaluator edgeEvaluator) {
    System.out.println("> Extend endpoint");
    Vertex endpointOrigin;
    if (endpoint == endpoint1)
      endpointOrigin = endpoint1Origin;
    else if (endpoint == endpoint2)
      endpointOrigin = endpoint2Origin;
    else
      throw new IllegalArgumentException("Vertex is not spline endpoint");
    if (isLeaf(endpointOrigin)) {
      System.out.println("> is leaf " + (endpoint == endpoint1 ? "e1" : "e2"));
      return false;
    }
    Edge newEdge = strongestValidEdge(endpointOrigin.getBranches(), endpoint, edgeEvaluator);
    if (newEdge == null)
      return false;
    addEdge(newEdge);
    return true;
  }

  public Edge strongestValidEdge (ArrayList<Edge> candidates, Vertex start, EdgeEvaluator edgeEvaluator) {
    System.out.println("> strongestValidEdge");
    double bestScore = Double.NEGATIVE_INFINITY;
    Edge bestEdge = null;
    for (Edge candidate : candidates) {
      // if valid edge
      System.out.println("> common vertices: " + commonVertices(candidate));
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
}
