package dev.mtbt.graph;

public interface EdgeEvaluator {
  double score (Edge edge, Vertex start);
}