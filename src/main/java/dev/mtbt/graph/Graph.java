package dev.mtbt.graph;

import dev.mtbt.Utils;
import dev.mtbt.util.Pair;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

public class Graph {
  protected TreeSet<Edge> edges;
  protected TreeSet<Vertex> vertices;

  public Graph() {
    this.edges = new TreeSet<>();
    this.vertices = new TreeSet<>();
  }

  public Graph(sc.fiji.analyzeSkeleton.Graph g) {
    this.edges = new TreeSet<>();
    this.vertices = new TreeSet<>();
    final Map<sc.fiji.analyzeSkeleton.Vertex, Vertex> vertexMap =
        g.getVertices().stream().collect(toMap(identity(), Vertex::new));
    final Function<sc.fiji.analyzeSkeleton.Edge, Edge> cloner =
        e -> new Edge(e, vertexMap.get(e.getV1()), vertexMap.get(e.getV2()));
    final Map<sc.fiji.analyzeSkeleton.Edge, Edge> edgeMap =
        g.getEdges().stream().collect(toMap(identity(), cloner));
    g.getEdges().stream().map(edgeMap::get).forEach(this::addEdge);
    g.getVertices().stream().map(vertexMap::get).forEach(this::addVertex);
  }

  public Edge addEdge(Edge e) {
    Edge newEdge = e;
    if (!this.edges.add(e))
      newEdge = this.edges.floor(e);
    newEdge.getV1().setBranch(e);
    newEdge.getV2().setBranch(e);
    return newEdge;
  }

  public Vertex addVertex(Vertex v) {
    if (!this.vertices.add(v))
      return this.vertices.floor(v);
    return v;
  }

  public TreeSet<Vertex> getVertices() {
    return this.vertices;
  }

  public TreeSet<Edge> getEdges() {
    return this.edges;
  }

  public Edge closestEdge(Point point) {
    return this.closestEdgeDistance(point).getKey();
  }

  public double distance(Point point) {
    return this.closestEdgeDistance(point).getValue();
  }

  public Pair<Edge, Double> closestEdgeDistance(Point point) {
    double bestDistance = Double.POSITIVE_INFINITY;
    Edge bestEdge = null;
    for (Edge edge : this.edges) {
      for (Point slab : edge.getSlabs()) {
        double dist = Utils.distance(point, slab);
        if (dist < bestDistance) {
          bestDistance = dist;
          bestEdge = edge;
        }
      }
    }
    return new Pair<>(bestEdge, bestDistance);
  }

  @Override
  public Graph clone() {
    final Graph clone = new Graph();
    final Map<Vertex, Vertex> vertexMap =
        vertices.stream().collect(toMap(identity(), Vertex::cloneUnconnected));
    final Function<Edge, Edge> cloner =
        e -> e.clone(vertexMap.get(e.getV1()), vertexMap.get(e.getV2()));
    final Map<Edge, Edge> edgeMap = edges.stream().collect(toMap(identity(), cloner));
    edges.stream().map(edgeMap::get).forEach(clone::addEdge);
    vertices.stream().map(vertexMap::get).forEach(clone::addVertex);
    return clone;
  }
}
