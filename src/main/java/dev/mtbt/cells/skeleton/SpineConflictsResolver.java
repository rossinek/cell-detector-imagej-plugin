package dev.mtbt.cells.skeleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import dev.mtbt.cells.skeleton.Spine.Path;
import dev.mtbt.cells.skeleton.SpineTraverser.SpineTraverserStep;
import dev.mtbt.graph.Edge;
import dev.mtbt.graph.IEdgeEvaluator;
import dev.mtbt.graph.IPointEvaluator;
import dev.mtbt.graph.Point;
import dev.mtbt.graph.Vertex;
import dev.mtbt.util.Pair;

public class SpineConflictsResolver {

  static public List<Pair<Point, Spine>> fixConflicts(List<Pair<Point, Spine>> spines,
      IPointEvaluator pointEvaluator, IEdgeEvaluator edgeEvaluator) {
    ListIterator<Pair<Point, Spine>> iterator = spines.listIterator();
    while (iterator.hasNext()) {
      Pair<Point, Spine> spine = iterator.next();
      for (int i = iterator.nextIndex(); i < spines.size(); i++) {
        boolean change;
        do {
          change = false;
          if (spine.getValue().overlaps(spines.get(i).getValue())) {
            Point p1 = spine.getKey();
            Point p2 = spines.get(i).getKey();
            Pair<Boolean, Point> result =
                SpineConflictsResolver.resolveConflict(new Pair<>(p1, spine.getValue()),
                    new Pair<>(p2, spines.get(i).getValue()), pointEvaluator, edgeEvaluator);
            change = result.getKey();
            Point cutPoint = result.getValue();
            if (cutPoint != null) {
              for (Pair<Point, Spine> pair : spines.subList(i + 1, spines.size())) {
                SpineConflictsResolver.cut(pair.getValue(), cutPoint, pair.getKey());
              }
            }
          }
        } while (change);
      }
    }
    return spines;
  }

  /**
   * Get the weakest point on spine between two points according to given score function
   */
  static private Point weakestPoint(List<Point> points, IPointEvaluator pointEvaluator) {
    Point weakest = points.get(0);
    double minScore = Double.POSITIVE_INFINITY;
    for (Point point : points) {
      double score = pointEvaluator.score(point);
      if (score < minScore) {
        minScore = score;
        weakest = point;
      }
    }
    return weakest;
  }

  static protected OverlapType overlapType(Spine s1, Spine s2) {
    Edge commonEdge1 = s1.getAnyCommonEdge(s2.getEdges().toArray(new Edge[s2.getEdges().size()]));
    if (commonEdge1 == null) {
      return OverlapType.None;
    }
    if ((s1.getE1().equals(s2.getE1()) && s1.getE2().equals(s2.getE2()))
        || (s1.getE1().equals(s2.getE2()) && s1.getE2().equals(s2.getE1()))) {
      return OverlapType.Full;
    }
    return OverlapType.Partial;
  }

  /*-
   * ASCI art symbols:
   *   *     vertex
   *   - \ / edges
   *   = ||  common edges
   *   x     reference point
   *
   * There are three options:
   * 1. FULL OVERLAP - split need to have
   *   *====x=====x====*
   *
   * 2. PARTIAL OVERLAP WITH COMMON DIRECTION
   * - common part have to be assigned to one of spines
   *  *--x--\     /-----*
   *         *===*
   *  *--x--/     \-----*
   *
   * 3. PARTIAL OVERLAP WITH OPPOSITE DIRECTION
   * - common part can be split somewhere between reference points
   *  *-----\     /--x--*
   *         *===*
   *  *--x--/     \-----*
   *
   * @return Pair<changed, cutPoint>
   * where `changed` indicates if any change occurred and
   * `cutPoint` is newly created vertex on edge cut (if any), otherwise null
  */
  static private Pair<Boolean, Point> resolveConflict(Pair<Point, Spine> s1, Pair<Point, Spine> s2,
      IPointEvaluator pointEvaluator, IEdgeEvaluator edgeEvaluator) {

    // 1. FIND OVERLAP PATH
    Path path = overlapPath(s1.getValue(), s2.getValue());
    List<Point> slabs = path.toSlabs(false);

    // 2. FIND BEGIN / END SLABS ACCORDING TO REFERENCE POINTS (OR ENDS)
    Pair<Point, Edge> ref1 = s1.getValue().closestEdge(s1.getKey());
    int indexOfRef1 = slabs.indexOf(ref1.getKey());
    Pair<Point, Edge> ref2 = s2.getValue().closestEdge(s2.getKey());
    int indexOfRef2 = slabs.indexOf(ref2.getKey());
    int bi = 0;
    int ei = slabs.size() - 1;
    if (indexOfRef1 >= 0 && indexOfRef2 >= 0) {
      System.out.println("FULL OVERLAP");
      bi = Math.min(indexOfRef1, indexOfRef2) + 1;
      ei = Math.max(indexOfRef1, indexOfRef2) - 1;
      if (bi >= ei) {
        // nothing to change...
        return new Pair<>(false, null);
      }
    } else if (indexOfRef1 >= 0 || indexOfRef2 >= 0) {
      System.out.println("PARTIAL OVERLAP - ref inside");
      Pair<Point, Edge> refOnCommonPart = indexOfRef1 >= 0 ? ref1 : ref2;
      Pair<Point, Edge> refOnPrivatePart = indexOfRef1 >= 0 ? ref2 : ref1;
      Spine refOnCommonPartSpine = indexOfRef1 >= 0 ? s1.getValue() : s2.getValue();
      Spine refOnPrivatePartSpine = indexOfRef1 >= 0 ? s2.getValue() : s1.getValue();
      int slabIndexOnCommonPart = indexOfRef1 >= 0 ? indexOfRef1 : indexOfRef2;

      int direction = referenceDirection(refOnPrivatePartSpine,
          refOnPrivatePartSpine.getEdges().floor(refOnCommonPart.getValue()), slabIndexOnCommonPart,
          refOnPrivatePart.getKey());
      int pathDirection = direction * edgeDirectionInPath(path, refOnCommonPart.getValue());
      if (pathDirection > 0 ? refOnCommonPartSpine.getVertices().floor(path.getBegin()).isLeaf()
          : refOnCommonPartSpine.getVertices().floor(path.getEnd()).isLeaf()) {
        // ASSIGN OVERLAP
        Vertex lastVertex = pathDirection > 0 ? path.getEnd() : path.getBegin();
        Edge lastCommonEdge = pathDirection > 0 ? path.getLastEdge() : path.getFirstEdge();
        Edge firstPrivateEdge =
            ((SpineVertex) refOnPrivatePartSpine.getVertices().floor(lastVertex))
                .getOppositeBranch(lastCommonEdge);
        cutFromEdge(refOnPrivatePartSpine, firstPrivateEdge,
            firstPrivateEdge.getV1().equals(lastVertex) ? -1 : 1);
        return new Pair<>(true, null);
      }
      if (pathDirection < 0) {
        ei = slabIndexOnCommonPart;
      } else {
        bi = slabIndexOnCommonPart;
      }
    } else {
      System.out.println("PARTIAL OVERLAP and both reference points are outside of common part");
      // PARTIAL OVERLAP and both reference points are outside of common part:
      // Check if overlap has common or opposite direction
      Edge firstCommonEdge1 = s1.getValue().getEdges().floor(path.getFirstEdge());
      SpineVertex firstCommonVertex1 =
          (SpineVertex) s1.getValue().getVertices().floor(path.getBegin());
      Edge firstCommonEdge2 = s2.getValue().getEdges().floor(path.getFirstEdge());
      SpineVertex firstCommonVertex2 =
          (SpineVertex) s2.getValue().getVertices().floor(path.getBegin());

      boolean dir1 = false;
      boolean dir2 = false;
      SpineTraverser t1 = new SpineTraverser(firstCommonVertex1, firstCommonEdge1);
      while (t1.hasNext()) {
        SpineTraverserStep step = t1.next();
        if (step.edge == ref1.getValue()) {
          dir1 = true;
          break;
        }
      }
      SpineTraverser t2 = new SpineTraverser(firstCommonVertex2, firstCommonEdge2);
      while (t2.hasNext()) {
        SpineTraverserStep step = t2.next();
        if (step.edge == ref2.getValue()) {
          dir2 = true;
          break;
        }
      }
      if (dir1 == dir2) {
        System.out.println("PARTIAL OVERLAP - ASSIGN OVERLAP");
        // ASSIGN OVERLAP TO ONE SPINE AND SHORTEN ANOTHER
        // 1. DECIDE WHICH SPINE SHOULD CONTAIN OVERLAP
        SpineVertex crossVertex = dir1 ? path.getEnd() : path.getBegin();
        Edge firstCommonEdge = dir1 ? path.getLastEdge() : path.getFirstEdge();
        SpineVertex crossVertex1 = (SpineVertex) s1.getValue().getVertices().floor(crossVertex);
        SpineVertex crossVertex2 = (SpineVertex) s2.getValue().getVertices().floor(crossVertex);
        Edge candidate1 = crossVertex1.getOppositeBranch(firstCommonEdge);
        Edge candidate2 = crossVertex2.getOppositeBranch(firstCommonEdge);

        Spine spineToShorten = s1.getValue();
        Edge weakerEdge = candidate1;
        if (edgeEvaluator.score(candidate1, crossVertex1) > edgeEvaluator.score(candidate2,
            crossVertex2)) {
          spineToShorten = s2.getValue();
          weakerEdge = candidate2;
        }
        // 2. REMOVE OVERLAPPING EDGES FROM SECOND SPINE
        cutFromEdge(spineToShorten, weakerEdge, crossVertex.equals(weakerEdge.getV1()) ? -1 : 1);
        return new Pair<>(true, null);
      }
      System.out.println("PARTIAL OVERLAP - opposite direction");
    }

    // 3. FIND WEAKEST POINTS OF COMMON PART BETWEEN REF POINTS
    Point cutPoint = weakestPoint(slabs.subList(bi, ei + 1), pointEvaluator);

    // 4. CUT SPINES AND RETURN NEW VERTEX (CUT POINT TO PROPAGATE) IF CREATED
    Pair<Boolean, Point> res1 = cut(s1.getValue(), cutPoint, s1.getKey());
    Pair<Boolean, Point> res2 = cut(s2.getValue(), cutPoint, s2.getKey());
    return new Pair<>(res1.getKey() || res2.getKey(),
        (res2.getValue() != null || res1.getValue() == null) ? cutPoint : null);
  }

  static private Path overlapPath(Spine s1, Spine s2) {
    // get overlapping edges that are not surrounded by overlapping edges from both sides
    List<Edge> overlapLastEdges = s1.getEdges().stream().filter(e1 -> {
      Edge e2 = s2.getEdges().floor(e1);
      if (e2 == null || !e1.equals(e2)) {
        return false;
      }
      Edge e1n1 = ((SpineVertex) e1.getV1()).getOppositeBranch(e1);
      Edge e1n2 = ((SpineVertex) e1.getV2()).getOppositeBranch(e1);
      Edge e2n1 = ((SpineVertex) e2.getV1()).getOppositeBranch(e2);
      Edge e2n2 = ((SpineVertex) e2.getV2()).getOppositeBranch(e2);

      // have different neighbors
      return e1n1 == null || e1n2 == null || e2n1 == null || e2n2 == null || !e1n1.equals(e2n1)
          || !e1n2.equals(e2n2);
    }).collect(Collectors.toList());

    if (overlapLastEdges.isEmpty()) {
      throw new IllegalArgumentException("No overlap!");
    }

    List<Vertex> overlapLastVertices = overlapLastEdges.stream().flatMap(e -> {
      List<Vertex> list = new ArrayList<>();
      Vertex v1 = s2.getVertices().floor(e.getV1());
      Vertex v2 = s2.getVertices().floor(e.getV2());
      if (v1 != null && v1.equals(e.getV1())) {
        list.add(e.getV1());
      }
      if (v2 != null && v2.equals(e.getV2())) {
        list.add(e.getV2());
      }
      return list.stream();
    }).distinct().collect(Collectors.toList());

    if (overlapLastVertices.size() > 2) {
      System.out.println("> Multi overlap");
      // MULTIPLE OVERLAPS
      // Prioritize overlap with spine endpoint
      Edge firstEdge =
          overlapLastEdges.stream().filter(e -> e.getV1().isLeaf() || e.getV2().isLeaf()).findAny()
              .orElse(overlapLastEdges.get(0));
      List<Vertex> potentialBegins =
          Arrays.stream(new Vertex[] {firstEdge.getV1(), firstEdge.getV2()})
              .filter(v -> overlapLastVertices.contains(v)).collect(Collectors.toList());
      Vertex firstVertex = potentialBegins.size() == 1 ? potentialBegins.get(0)
          : potentialBegins.stream().filter(v -> v.isLeaf()).findAny()
              .orElse(potentialBegins.get(0));
      Path path = new Path((SpineVertex) firstVertex,
          (SpineVertex) firstEdge.getOppositeVertex(firstVertex), firstEdge, firstEdge);

      Edge newEdge = path.getEnd().getOppositeBranch(path.getLastEdge());
      while (newEdge != null && s2.getEdges().floor(newEdge) != null
          && s2.getEdges().floor(newEdge).equals(newEdge)) {
        path.extendEnd();
        newEdge = path.getEnd().getOppositeBranch(path.getLastEdge());
      }
      return path;
    }
    return s1.findPath(overlapLastEdges.get(0),
        overlapLastEdges.size() > 1 ? overlapLastEdges.get(1) : overlapLastEdges.get(0));
  }

  static protected Edge[] splitEdge(Edge e, int slabIndex) {
    if (slabIndex <= 0 || slabIndex >= e.getSlabs().size() - 1)
      throw new IllegalArgumentException();
    ArrayList<Point> slabs1 = new ArrayList<>();
    ArrayList<Point> slabs2 = new ArrayList<>();
    if (slabIndex >= 0) {
      slabs1.addAll(e.getSlabs().subList(0, slabIndex));
      slabs2.addAll(e.getSlabs().subList(slabIndex + 1, e.getSlabs().size()));
    }
    Vertex vSlab = new SpineVertex();
    vSlab.addPoint(e.getSlabs().get(slabIndex));
    Vertex v1 = e.getV1().cloneUnconnected();
    e.getV1().getBranches().forEach(branch -> {
      if (branch != e) {
        v1.setBranch(branch);
      }
    });

    Vertex v2 = e.getV2().cloneUnconnected();
    e.getV2().getBranches().forEach(branch -> {
      if (branch != e) {
        v1.setBranch(branch);
      }
    });

    // Vertex vSlab2 = vSlab.cloneUnconnected();
    Edge edge1 = new Edge(vSlab, v1, slabs1);
    Edge edge2 = new Edge(vSlab, v2, slabs2);

    v1.setBranch(edge1);
    v2.setBranch(edge2);
    vSlab.setBranch(edge1);
    vSlab.setBranch(edge2);
    return new Edge[] {edge1, edge2};
  }

  /**
   * cut spine from a given slab, keep part that contains reference point
   *
   * @return Pair<changed, cutPoint> where `changed` indicates if any change occurred and `cutPoint`
   *         is newly created vertex on edge cut (if any), otherwise null
   */
  static private Pair<Boolean, Point> cut(Spine spine, Point slab, Point referencePoint) {
    Edge edge = spine.findEdge(slab);
    if (edge == null) {
      return new Pair<>(false, null);
    }
    int indexOfSlab = edge.getSlabs().indexOf(slab);
    if (indexOfSlab < 3) {
      int removedEdges = cutOnVertex(spine, (SpineVertex) edge.getV1(), referencePoint);
      return new Pair<>(removedEdges > 0, null);
    }
    if (indexOfSlab > edge.getSlabs().size() - 4) {
      int removedEdges = cutOnVertex(spine, (SpineVertex) edge.getV2(), referencePoint);
      return new Pair<>(removedEdges > 0, null);
    }
    cutOnEdge(spine, edge, indexOfSlab, referencePoint);
    return new Pair<>(true, slab);
  }

  /**
   * @return number of removed edges
   */
  static private int cutOnVertex(Spine spine, SpineVertex vertex, Point referencePoint) {
    System.out.println(">>>> cutOnVertex");
    Pair<Point, Edge> ref = spine.closestEdge(referencePoint);
    SpineTraverserStep stepWithVertex = null;

    SpineTraverser st = new SpineTraverser((SpineVertex) ref.getValue().getV1(), ref.getValue());
    while (st.hasNext()) {
      SpineTraverserStep step = st.next();
      if (step.v2.equals(vertex)) {
        stepWithVertex = step;
      }
    }
    if (stepWithVertex == null) {
      st = new SpineTraverser((SpineVertex) ref.getValue().getV2(), ref.getValue());
      while (st.hasNext()) {
        SpineTraverserStep step = st.next();
        if (step.v2.equals(vertex)) {
          stepWithVertex = step;
        }
      }
    }
    if (stepWithVertex != null) {
      int direction = stepWithVertex.edge.getV2().equals(vertex) ? 1 : -1;
      return cutFromEdge(spine, stepWithVertex.edge, direction);
    }
    return 0;
  }

  static private void cutOnEdge(Spine spine, Edge edge, int slabIndex, Point referencePoint) {
    int direction = referenceDirection(spine, edge, slabIndex, referencePoint);
    SpineVertex preservedPoint = (SpineVertex) (direction > 0 ? edge.getV2() : edge.getV1());
    cutFromEdge(spine, edge, -1 * direction);
    spine.shorten((SpineVertex) edge.getOppositeVertex(preservedPoint));
    Edge[] edges = SpineConflictsResolver.splitEdge(edge, slabIndex);
    spine.addEdge(direction > 0 ? edges[1] : edges[0]);
  }

  /**
   * keep edge but cut everything from its vertex (according to direction) returns number of removed
   * edges
   */
  static private int cutFromEdge(Spine spine, Edge edge, int direction) {
    SpineVertex newEndpoint = (SpineVertex) (direction > 0 ? edge.getV2() : edge.getV1());
    SpineTraverser traverser =
        new SpineTraverser((SpineVertex) edge.getOppositeVertex(newEndpoint), edge);
    SpineVertex currentEndpoint = newEndpoint;
    while (traverser.hasNext()) {
      SpineTraverserStep step = traverser.next();
      currentEndpoint = step.v2;
    }
    int removedEdges = 0;
    while (!currentEndpoint.equals(newEndpoint)) {
      currentEndpoint = spine.shorten(currentEndpoint);
    }
    return removedEdges;
  }

  /**
   * Get direction from slabIndex to slab closest to referencePoint. Returns -1 or +1 according to
   * slab indices (e.g. -1 means direction to v1)
   */
  static private int referenceDirection(Spine spine, Edge edge, int slabIndex,
      Point referencePoint) {
    Pair<Point, Edge> reference = spine.closestEdge(referencePoint);
    if (reference.getValue().equals(edge)) {
      int indexOfRefSlab = edge.getSlabs().indexOf(reference.getKey());
      return indexOfRefSlab < slabIndex ? -1 : 1;
    }
    int direction = 1;
    SpineTraverser traverser = new SpineTraverser((SpineVertex) edge.getV2(), edge);
    while (traverser.hasNext()) {
      SpineTraverserStep step = traverser.next();
      if (step.edge.equals(reference.getValue())) {
        direction = -1;
      }
    }
    return direction;
  }

  // return 1 for same direction as edge.v1 -> edge.v2 order, -1 otherwise
  static private int edgeDirectionInPath(Path path, Edge edge) {
    SpineTraverser traverser =
        new SpineTraverser(path.getBegin(), path.getFirstEdge(), path.getEnd());
    while (traverser.hasNext()) {
      SpineTraverserStep step = traverser.next();
      if (step.edge.equals(edge)) {
        return edge.getV1().equals(step.v1) ? 1 : -1;
      }
    }
    throw new IllegalArgumentException("No such edge");
  }

  static public enum OverlapType {
    None, Partial, Full
  }
}
