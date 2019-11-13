package dev.mtbt.cells.skeleton;

import dev.mtbt.graph.Edge;
import dev.mtbt.graph.Point;
import dev.mtbt.graph.Vertex;

import static java.util.stream.Collectors.toList;

public class SpineVertex extends Vertex {
  private Vertex skeletonVertex = null;

  public SpineVertex() {
    super();
  }

  public SpineVertex(Vertex origin) {
    super();
    if (origin != null) {
      this.points.addAll(origin.getPoints());
      this.skeletonVertex = origin;
    }
  }

  public boolean isSkeletonVertex() {
    return this.skeletonVertex != null;
  }

  public Edge getOppositeBranch(Edge e) {
    if (this.branches.size() < 2)
      return null;
    return this.branches.first().equals(e) ? this.branches.last() : this.branches.first();
  }

  public Vertex getSkeletonVertex() {
    return this.skeletonVertex;
  }

  @Override
  public void setBranch(Edge e) {
    // if (isSkeletonVertex() && !this.skeletonVertex.getBranches().contains(e)) {
    // throw new IllegalArgumentException("Edge is not the part of spine's skeleton");
    // }
    this.branches.add(e);
  }

  @Override
  public Vertex cloneUnconnected() {
    final SpineVertex clone = new SpineVertex(this.skeletonVertex);
    clone.points.clear();
    clone.points.addAll(points.stream().map(Point::clone).collect(toList()));
    return clone;
  }
}
