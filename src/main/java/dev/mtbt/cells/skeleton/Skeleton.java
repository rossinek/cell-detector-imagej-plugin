package dev.mtbt.cells.skeleton;

import dev.mtbt.graph.Graph;
import dev.mtbt.graph.Edge;
import dev.mtbt.graph.Point;
import dev.mtbt.graph.Vertex;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.SkeletonResult;
import sc.fiji.skeletonize3D.Skeletonize3D_;

import java.util.*;
import java.util.function.Function;

import static dev.mtbt.Utils.distance;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class Skeleton extends Graph {
  protected FloatProcessor initialFp;
  protected AnalyzeSkeleton_ analyzeSkeleton;
  protected SkeletonResult skeletonResult;

  public Skeleton (ImagePlus imp) {
    super();
    initialFp = imp.getProcessor().convertToFloatProcessor();
    ImagePlus impSkeleton = new ImagePlus("skeleton", imp.getProcessor().convertToByteProcessor());

    Skeletonize3D_ skeletonizer = new Skeletonize3D_();
    skeletonizer.setup("", impSkeleton);
    skeletonizer.run(impSkeleton.getProcessor());

    analyzeSkeleton = new AnalyzeSkeleton_();
    analyzeSkeleton.setup("", impSkeleton);
    skeletonResult = analyzeSkeleton.run(AnalyzeSkeleton_.NONE, false, false, impSkeleton, true, false);

    // Clone all graphs to create skeleton
    Set<Graph> graphs = Arrays.stream(analyzeSkeleton.getGraphs()).map(Graph::new).collect(toSet());
    Map<Vertex, Vertex> vertexMap = new HashMap<>();
    Map<Edge, Edge> edgeMap = new HashMap<>();
    graphs.forEach(g -> vertexMap.putAll(g.getVertices().stream().collect(toMap(identity(), Vertex::cloneUnconnected))));
    Function<Edge, Edge> cloner = e -> e.clone(vertexMap.get(e.getV1()), vertexMap.get(e.getV2()));
    graphs.forEach(g -> edgeMap.putAll(g.getEdges().stream().collect(toMap(identity(), cloner))));
    edgeMap.keySet().stream().map(edgeMap::get).forEach(this::addEdge);
    vertexMap.keySet().stream().map(vertexMap::get).forEach(this::addVertex);
  }

  /**
   * Find finite poly line containing point
   *
   * @param initialPoint should lay on skeleton
   * @return poly line containing point
   */
  public Spine findSpine (java.awt.Point initialPoint) {
    Edge initialEdge = this.closestEdge(new Point(initialPoint));
    Spine spine = new Spine();
    if (initialEdge != null) {
      spine.addEdge(initialEdge);
      spine.extend((edge, start) -> {
        final double RADIUS = 10;
        double lowestValue = Double.POSITIVE_INFINITY;
        for (Point slab : edge.getSlabs()) {
          double dist = distance(slab, start.getPoints());
          if (dist > RADIUS)
            continue;
          lowestValue = Math.min(initialFp.getf(slab.x, slab.y), lowestValue);
        }
        return lowestValue;
      });
    }
    return spine;
  }

  public ImagePlus toImagePlus () {
    ByteProcessor skeleton = (ByteProcessor) analyzeSkeleton.getResultImage(false).getProcessor(1);
    return new ImagePlus("Skeleton", skeleton);
  }
}
