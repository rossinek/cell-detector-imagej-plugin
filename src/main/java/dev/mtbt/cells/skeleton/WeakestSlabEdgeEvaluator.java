package dev.mtbt.cells.skeleton;

import dev.mtbt.graph.Edge;
import dev.mtbt.graph.IEdgeEvaluator;
import dev.mtbt.graph.Point;
import dev.mtbt.graph.Vertex;
import dev.mtbt.util.Geometry;
import ij.process.FloatProcessor;

public class WeakestSlabEdgeEvaluator implements IEdgeEvaluator {
  private static final double EDGE_EVALUATOR_RADIUS = 10;

  private FloatProcessor fp;

  public WeakestSlabEdgeEvaluator(FloatProcessor fp) {
    this.fp = fp;
  }

  @Override
  public double score(Edge edge, Vertex start) {
    double lowestValue = Double.POSITIVE_INFINITY;
    for (Point slab : edge.getSlabs()) {
      double dist = Geometry.distance(slab, start.getPoints());
      if (dist <= EDGE_EVALUATOR_RADIUS) {
        lowestValue = Math.min(fp.getf(slab.x, slab.y), lowestValue);
      }
    }
    double scale = 1 / (fp.getMax() - fp.getMin());
    return (lowestValue - fp.getMin()) * scale;
  }

}
