package dev.mtbt.cells.skeleton;

import dev.mtbt.graph.*;
import java.util.*;

public class SpineTraverser implements Iterator<SpineTraverser.SpineTraverserStep> {
  SpineVertex current;
  Edge nextEdge;
  SpineVertex end;


  public SpineTraverser(Spine spine) {
    this.current = spine.getE1();
    this.nextEdge = spine.getE1().getBranches().first();
  }

  public SpineTraverser(SpineVertex begin, Edge firstEdge) {
    this(begin, firstEdge, null);
  }

  public SpineTraverser(SpineVertex begin, Edge firstEdge, SpineVertex end) {
    this.current = begin;
    this.nextEdge = firstEdge;
    this.end = end;
  }

  public class SpineTraverserStep {
    SpineVertex v1;
    SpineVertex v2;
    Edge edge;

    private SpineTraverserStep(SpineVertex v1, SpineVertex v2, Edge edge) {
      this.v1 = v1;
      this.v2 = v2;
      this.edge = edge;
    }
  }

  @Override
  public boolean hasNext() {
    return (this.end == null || !this.current.equals(end)) && this.nextEdge != null;
  }

  @Override
  public SpineTraverserStep next() {
    if (!this.hasNext())
      throw new NoSuchElementException();
    SpineTraverserStep step = new SpineTraverserStep(current,
        (SpineVertex) this.nextEdge.getOppositeVertex(current), this.nextEdge);
    this.current = (SpineVertex) this.nextEdge.getOppositeVertex(current);
    this.nextEdge = this.current.getOppositeBranch(this.nextEdge);
    return step;
  }
}
