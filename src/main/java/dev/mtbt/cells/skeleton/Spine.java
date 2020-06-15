package dev.mtbt.cells.skeleton;

import dev.mtbt.graph.*;
import dev.mtbt.util.Pair;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;
import dev.mtbt.Utils;
import dev.mtbt.cells.skeleton.SpineTraverser.SpineTraverserStep;

public class Spine extends Graph {

  private SpineVertex e1 = null;
  private SpineVertex e2 = null;

  public Spine() {
    super();
  }

  public SpineVertex getE1() {
    return this.e1;
  }

  public SpineVertex getE2() {
    return this.e2;
  }

  public List<Point2D> toPolyline() {
    ArrayList<Point> points = new ArrayList<>();
    if (this.e1 != null) {
      points = this.toPath().toSlabs(true);
      points = Utils.simplifyPolyline(points, 2);
    }
    return points.stream().map(p -> p.toPoint2D()).collect(Collectors.toCollection(ArrayList::new));
  }

  public void reverse() {
    SpineVertex temp = this.e1;
    this.e1 = this.e2;
    this.e2 = temp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!Spine.class.isAssignableFrom(o.getClass()))
      return false;
    final Spine s = (Spine) o;
    return Spine.overlapType(this, s) == OverlapType.Full;
  }

  // public void traverse(GraphTraverser t) {
  // traverse(this.e1, this.e1.getBranches().first(), t);
  // }

  // public void traverse(SpineVertex begin, Edge firstEdge, GraphTraverser t) {
  // SpineVertex current = begin;
  // Edge nextEdge = firstEdge;
  // while (true) {
  // t.callback(current, nextEdge.getOppositeVertex(current), nextEdge);
  // current = (SpineVertex) nextEdge.getOppositeVertex(current);
  // if (current.isLeaf())
  // break;
  // nextEdge = current.getOppositeBranch(nextEdge);
  // }
  // }

  @Override
  public Edge addEdge(Edge e) {
    validateNewEdge(e);
    SpineVertex v1 = (SpineVertex) this.addVertex(new SpineVertex(e.getV1()));
    SpineVertex v2 = (SpineVertex) this.addVertex(new SpineVertex(e.getV2()));
    Edge edge = e.clone(v1, v2);
    if (this.edges.size() == 0) {
      this.e1 = v1;
      this.e2 = v2;
    } else if (edge.isIncidentTo(this.e1)) {
      this.e1 = (SpineVertex) edge.getOppositeVertex(this.e1);
    } else if (edge.isIncidentTo(this.e2)) {
      this.e2 = (SpineVertex) edge.getOppositeVertex(this.e2);
    }
    return super.addEdge(edge);
  }

  public boolean overlaps(Spine spine) {
    return overlapType(this, spine) != OverlapType.None;
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

  static public OverlapType overlapType(Spine s1, Spine s2) {
    int ces = s1.commonEdges(s2.edges.toArray(new Edge[s2.edges.size()]));
    if (ces == 0) {
      return OverlapType.None;
    }
    int cvs = 0;
    cvs += Arrays.asList(new Vertex[] {s1.e1, s1.e2}).contains(s2.e1) ? 1 : 0;
    cvs += Arrays.asList(new Vertex[] {s1.e1, s1.e2}).contains(s2.e2) ? 1 : 0;
    if (cvs == 0) {
      return OverlapType.Partial;
    }
    if (cvs == 1) {
      return OverlapType.PartialWithVertex;
    }
    return OverlapType.Full;
  }

  /**
   * Split Spine in the weakest point between points
   *
   * @return array containing two spines: first containing p1, second containing p2
   */
  static public void splitOverlap(Pair<Point, Spine> s1, Pair<Point, Spine> s2,
      IPointEvaluator pointEvaluator) {
    OverlapType type = overlapType(s1.getValue(), s2.getValue());
    switch (type) {
      case Full:
      case Partial:
        splitOverlapCenter(s1, s2, pointEvaluator);
        break;
      case PartialWithVertex:
        assignOverlapByReference(s1, s2, pointEvaluator);
        break;
      default:
        break;
    }
  }

  static private void splitOverlapCenter(Pair<Point, Spine> s1, Pair<Point, Spine> s2,
      IPointEvaluator pointEvaluator) {

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
      bi = Math.min(indexOfRef1, indexOfRef2) + 1;
      ei = Math.max(indexOfRef1, indexOfRef2) - 1;
      if (bi >= ei) {
        throw new IllegalArgumentException("Sorry something not handled here :(");
      }
    } else if (indexOfRef1 >= 0 || indexOfRef2 >= 0) {
      // Assumptions:
      // 1) this method is implemented for partial and full overlap
      // 2) full overlap => both reference slabs are inside slabs
      // 3) partial overlap => both reference slabs are outside slabs
      // because of the implementation of spine creation (see this::extend)
      throw new IllegalArgumentException("Implemented only for Full & Partial overlap");
    }

    // 3. FIND WEAKEST POINTS BETWEEN
    Point cutPoint = weakestPoint(slabs.subList(bi, ei + 1), pointEvaluator);

    // 4. CUT BOTH SPINES
    s1.getValue().cut(cutPoint, s1.getKey());
    s2.getValue().cut(cutPoint, s2.getKey());
  }

  static private void assignOverlapByReference(Pair<Point, Spine> s1, Pair<Point, Spine> s2,
      IPointEvaluator pointEvaluator) {
    // 1. DECIDE WHICH SPINE SHOULD CONTAIN OVERLAP
    Path path = overlapPath(s1.getValue(), s2.getValue());
    List<Point> slabs = path.toSlabs(false);
    Pair<Point, Edge> ref1 = s1.getValue().closestEdge(s1.getKey());
    Spine spineToShorten = slabs.contains(ref1.getKey()) ? s2.getValue() : s1.getValue();

    // 2. REMOVE OVERLAPPING EDGES FROM SECOND SPINE
    SpineVertex pathBegin = (SpineVertex) spineToShorten.vertices.floor(path.getBegin());
    SpineVertex pathEnd = (SpineVertex) spineToShorten.vertices.floor(path.getEnd());
    SpineVertex crossVertex = pathBegin.isLeaf() ? pathEnd : pathBegin;
    Edge lastEdge = pathBegin.isLeaf() ? crossVertex.getOppositeBranch(path.getLastEdge())
        : crossVertex.getOppositeBranch(path.getFirstEdge());
    spineToShorten.cutFromEdge(lastEdge, crossVertex.equals(lastEdge.getV1()) ? -1 : 1);

    // 3. EXTEND SECOND SPINE IF POSSIBLE
    // TODO
  }

  static private Path overlapPath(Spine s1, Spine s2) {
    // get overlapping edges that are not surrounded by overlapping edges from both sides
    List<Edge> overlapEnds = s1.edges.stream().filter(e1 -> {
      Edge e2 = s2.edges.floor(e1);
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
    if (overlapEnds.isEmpty()) {
      throw new IllegalArgumentException("No overlap!");
    } else if (overlapEnds.size() > 2) {
      /*-
       * there is a case where it is possible:
       *
       *  *---@---\
       *     ||    @==@
       *  *---@---/
       *
       * where
       *   *     is vertex
       *   - \ / are edges
       *   @     is common vertex
       *   = ||  are common edges
       *
       * in such case we prioritize PartialWithVertex overlap
       */
      Edge lastEdge = overlapEnds.stream().filter(e -> e.getV1().isLeaf() || e.getV2().isLeaf())
          .findAny().orElse(null);
      if (lastEdge == null) {
        throw new IllegalArgumentException("This should not happen");
      }
      Vertex begin;
      if (lastEdge.getV1().isLeaf()) {
        begin = s1.getE1().equals(lastEdge.getV1()) ? s1.getE1() : s1.getE2();
      } else {
        begin = s1.getE1().equals(lastEdge.getV2()) ? s1.getE1() : s1.getE2();
      }
      Path path = new Path((SpineVertex) begin,
          (SpineVertex) begin.getBranches().first().getOppositeVertex(begin),
          begin.getBranches().first(), begin.getBranches().first());

      Edge newEdge = path.getEnd().getOppositeBranch(path.getLastEdge());
      while (newEdge != null && s2.edges.floor(newEdge).equals(newEdge)) {
        path.extendEnd();
        newEdge = path.getEnd().getOppositeBranch(path.getLastEdge());
      }
      return path;
    }
    return s1.findPath(overlapEnds.get(0),
        overlapEnds.size() > 1 ? overlapEnds.get(1) : overlapEnds.get(0));
  }

  private void cut(Point slab, Point referencePoint) {
    Edge edge = findEdge(slab);
    int indexOfSlab = edge.getSlabs().indexOf(slab);
    if (indexOfSlab == 0) {
      this.cutOnVertex((SpineVertex) edge.getV1(), referencePoint);
    } else if (indexOfSlab == edge.getSlabs().size() - 1) {
      this.cutOnVertex((SpineVertex) edge.getV2(), referencePoint);
    } else {
      this.cutOnEdge(edge, indexOfSlab, referencePoint);
    }
  }

  private void cutOnVertex(SpineVertex vertex, Point referencePoint) {
    Pair<Point, Edge> ref = this.closestEdge(referencePoint);
    SpineTraverserStep stepWithVertex = null;

    SpineTraverser st = new SpineTraverser((SpineVertex) ref.getValue().getV1(), ref.getValue());
    for (SpineTraverserStep step = st.next(); st.hasNext(); step = st.next()) {
      if (step.v2.equals(vertex)) {
        stepWithVertex = step;
      }
    }
    if (stepWithVertex == null) {
      st = new SpineTraverser((SpineVertex) ref.getValue().getV2(), ref.getValue());
      for (SpineTraverserStep step = st.next(); st.hasNext(); step = st.next()) {
        if (step.v2.equals(vertex)) {
          stepWithVertex = step;
        }
      }
    }
    if (stepWithVertex != null) {
      int direction = stepWithVertex.edge.getV2().equals(vertex) ? 1 : -1;
      this.cutFromEdge(stepWithVertex.edge, direction);
    }
  }

  private void cutOnEdge(Edge edge, int slabIndex, Point referencePoint) {
    int direction = this.referenceDirection(edge, slabIndex, referencePoint);
    SpineVertex preservedPoint = (SpineVertex) (direction > 0 ? edge.getV2() : edge.getV1());
    this.cutFromEdge(edge, -1 * direction);
    this.shorten((SpineVertex) edge.getOppositeVertex(preservedPoint));
    Edge[] edges = splitEdge(edge, slabIndex);
    this.addEdge(direction > 0 ? edges[1] : edges[0]);
  }

  /**
   * keep edge but cut everything from its vertex (according to direction)
   */
  private void cutFromEdge(Edge edge, int direction) {
    SpineVertex newEndpoint = (SpineVertex) (direction > 0 ? edge.getV2() : edge.getV1());
    SpineTraverser traverser =
        new SpineTraverser((SpineVertex) edge.getOppositeVertex(newEndpoint), edge);
    SpineVertex currentEndpoint = newEndpoint;
    while (traverser.hasNext()) {
      SpineTraverserStep step = traverser.next();
      currentEndpoint = step.v2;
    }
    while (!currentEndpoint.equals(newEndpoint)) {
      currentEndpoint = this.shorten(currentEndpoint);
    }
  }

  /**
   * returns -1 or +1 according to slab indices (e.g. -1 means direction to v1)
   */
  private int referenceDirection(Edge edge, int slabIndex, Point referencePoint) {
    Pair<Point, Edge> reference = closestEdge(referencePoint);
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

  /**
   * Extend spine based on original graph
   */
  public void extend(IEdgeEvaluator edgeEvaluator) {
    while (extend(e1, edgeEvaluator));
    while (extend(e2, edgeEvaluator));
  }

  private Edge strongestValidEdge(Set<Edge> candidates, Vertex start,
      IEdgeEvaluator edgeEvaluator) {
    double bestScore = Double.NEGATIVE_INFINITY;
    Edge bestEdge = null;
    for (Edge candidate : candidates) {
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

  public boolean extend(Vertex endpoint, IEdgeEvaluator edgeEvaluator) {
    Vertex endpointOrigin;
    if (this.e1.equals(endpoint))
      endpointOrigin = this.e1.getSkeletonVertex();
    else if (this.e2.equals(endpoint))
      endpointOrigin = this.e2.getSkeletonVertex();
    else
      throw new IllegalArgumentException("Vertex is not spine endpoint");
    if (endpointOrigin.isLeaf())
      return false;
    Edge newEdge = strongestValidEdge(endpointOrigin.getBranches(), endpoint, edgeEvaluator);
    if (newEdge == null)
      return false;
    return addEdge(newEdge) != null;
  }

  private void validateNewEdge(Edge e) {
    if (this.edges.size() == 0)
      return;
    if (commonVertices(e) > 1) {
      throw new IllegalArgumentException("Edge is creating cycle in Spine");
    }
    if (!e.getV1().equals(this.e1) && !e.getV1().equals(this.e2) && !e.getV2().equals(this.e1)
        && !e.getV2().equals(this.e2)) {
      throw new IllegalArgumentException("Edge is not connected with Spine endpoints");
    }
  }

  private SpineVertex shorten(SpineVertex endpoint) {
    if (this.edges.size() < 1) {
      throw new IllegalArgumentException("No edges!");
    }
    if (!endpoint.equals(this.e1) && !endpoint.equals(this.e2)) {
      throw new IllegalArgumentException("Vertex is not spine endpoint");
    }
    if (this.edges.size() == 1) {
      this.edges.clear();
      this.vertices.clear();
      this.e1 = null;
      this.e2 = null;
      return null;
    }
    Edge edge = endpoint.getBranches().first();
    SpineVertex newEndpoint = (SpineVertex) edge.getOppositeVertex(endpoint);
    newEndpoint.getBranches().remove(edge);
    this.edges.remove(edge);
    this.vertices.remove(endpoint);
    if (endpoint.equals(this.e1)) {
      this.e1 = newEndpoint;
    } else {
      this.e2 = newEndpoint;
    }
    return newEndpoint;
  }

  static private Edge[] splitEdge(Edge e, int slabIndex) {
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

  private int commonVertices(Edge edge) {
    return commonVertices(new Vertex[] {edge.getV1(), edge.getV2()});
  }

  private int commonVertices(Vertex[] vertices) {
    int count = 0;
    for (Vertex vertex : vertices) {
      if (this.vertices.contains(vertex))
        count++;
    }
    return count;
  }

  private int commonEdges(Edge[] edges) {
    int count = 0;
    for (Edge edge : edges) {
      if (this.edges.contains(edge))
        count++;
    }
    return count;
  }

  private Edge findEdge(Point slab) {
    for (Edge edge : edges) {
      for (Point p : edge.getSlabs()) {
        if (p.equals(slab))
          return edge;
      }
    }
    return null;
  }

  private Path toPath() {
    return new Path(this.e1, this.e2, this.e1.getBranches().first(), this.e2.getBranches().first());
  }

  private Path findPath(Edge e1, Edge e2) {
    if (e1.equals(e2)) {
      return new Path((SpineVertex) e1.getV1(), (SpineVertex) e1.getV2(), e1, e2);
    }
    Path path;
    if (e1.getV1().equals(e2.getV2())) {
      path = findPath((SpineVertex) e1.getV2(), (SpineVertex) e2.getV1());
    } else {
      path = findPath((SpineVertex) e1.getV1(), (SpineVertex) e2.getV2());
    }

    if (path == null)
      return null;
    if (!path.getFirstEdge().equals(e1))
      path.extendBegin();
    if (!path.getLastEdge().equals(e2))
      path.extendEnd();
    return path;
  }

  private Path findPath(SpineVertex v1, SpineVertex v2) {
    for (Edge branch : v1.getBranches()) {
      SpineVertex current = v1;
      Edge nextEdge = branch;
      while (true) {
        current = (SpineVertex) nextEdge.getOppositeVertex(current);
        if (current.equals(v2)) {
          return new Path(v1, v2, branch, nextEdge);
        }
        if (current.isLeaf())
          break;
        nextEdge = current.getOppositeBranch(nextEdge);
      }
    }
    return null;
  }

  public java.awt.geom.Point2D.Double getBegin() {
    if (this.e1 == null)
      return null;
    return this.e1.getSkeletonVertex().center().toPoint2D();
  }

  public java.awt.geom.Point2D.Double getEnd() {
    if (this.e2 == null)
      return null;
    return this.e2.getSkeletonVertex().center().toPoint2D();
  }

  public void assign(Spine s) {
    super.assign(s);
    this.e1 = s.e1;
    this.e2 = s.e2;
  }

  public enum OverlapType {
    None, Partial, PartialWithVertex, Full,
  }

  static class Path {
    private SpineVertex begin;
    private SpineVertex end;
    private Edge firstEdge;
    private Edge lastEdge;

    public Path(SpineVertex begin, SpineVertex end, Edge firstEdge, Edge lastEdge) {
      this.begin = begin;
      this.end = end;
      this.firstEdge = firstEdge;
      this.lastEdge = lastEdge;
    }

    public SpineVertex getBegin() {
      return begin;
    }

    public SpineVertex getEnd() {
      return end;
    }

    public Edge getFirstEdge() {
      return firstEdge;
    }

    public Edge getLastEdge() {
      return lastEdge;
    }

    public void extendBegin() {
      if (!begin.isLeaf()) {
        firstEdge = begin.getOppositeBranch(firstEdge);
        begin = (SpineVertex) firstEdge.getOppositeVertex(begin);
      }
    }

    public void extendEnd() {
      if (!end.isLeaf()) {
        lastEdge = end.getOppositeBranch(lastEdge);
        end = (SpineVertex) lastEdge.getOppositeVertex(end);
      }
    }

    public ArrayList<Point> toSlabs(boolean addEndpoints) {
      ArrayList<Point> points = new ArrayList<>();

      if (addEndpoints)
        points.add(begin.center());

      SpineTraverser traverser = new SpineTraverser(begin, firstEdge, end);
      while (traverser.hasNext()) {
        SpineTraverserStep step = traverser.next();
        points.addAll(step.edge.getDirectedSlabs(step.v1));
      }
      if (addEndpoints)
        points.add(end.center());
      return points;
    }
  }
}
