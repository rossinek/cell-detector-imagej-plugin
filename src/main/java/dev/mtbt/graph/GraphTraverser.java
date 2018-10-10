package dev.mtbt.graph;

public interface GraphTraverser {
  void callback (Vertex v1, Vertex v2, Edge e);
}