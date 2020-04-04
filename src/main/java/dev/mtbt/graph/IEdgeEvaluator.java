package dev.mtbt.graph;

public interface IEdgeEvaluator {
  double score(Edge edge, Vertex start);
}
