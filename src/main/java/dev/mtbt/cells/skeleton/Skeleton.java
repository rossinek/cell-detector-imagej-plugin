package dev.mtbt.cells.skeleton;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import sc.fiji.analyzeSkeleton.*;
import sc.fiji.skeletonize3D.Skeletonize3D_;

import static dev.mtbt.cells.skeleton.Utils.distance;

public class Skeleton {
  private ImagePlus initialImage;
  private FloatProcessor initialFp;
  private Skeletonize3D_ skeletonizer;
  private AnalyzeSkeleton_ analyzeSkeleton;
  private SkeletonResult skeletonResult;

  public Skeleton (ImagePlus imp) {
    initialImage = imp.duplicate();
    initialFp = imp.getProcessor().convertToFloatProcessor();
    ImagePlus impSkeleton = new ImagePlus("skeleton", imp.getProcessor().convertToByteProcessor());

    skeletonizer = new Skeletonize3D_();
    skeletonizer.setup("", impSkeleton);
    skeletonizer.run(impSkeleton.getProcessor());

    analyzeSkeleton = new AnalyzeSkeleton_();
    analyzeSkeleton.setup("", impSkeleton);
    skeletonResult = analyzeSkeleton.run(AnalyzeSkeleton_.NONE, false, false, impSkeleton, true, false);
  }

  /**
   * Find finite poly line containing point
   *
   * @param initialPoint should lay on skeleton
   * @return poly line containing point
   */
  public Spine findSpine (java.awt.Point initialPoint) {
    Edge initialEdge = closestEdge(convertPoint(initialPoint));
    Spine spine = new Spine(analyzeSkeleton.getGraphs());
    if (initialEdge != null) {
      spine.addEdge(initialEdge);
      spine.extend((edge, start) -> {
        final double RADIUS = 10;
        double lowestValue = Double.POSITIVE_INFINITY;
        int n = 0;
        for (Point slab : edge.getSlabs()) {
          double dist = distance(slab, start.getPoints());
          if (dist > RADIUS)
            continue;
          lowestValue = Math.min(initialFp.getf(slab.x, slab.y), lowestValue);
          n++;
        }
        System.out.println("> edge score based on " + n + " values.");
        return lowestValue;
      });
    }
    return spine;
  }

  public ImagePlus toImagePlus () {
    ByteProcessor skeleton = (ByteProcessor) analyzeSkeleton.getResultImage(false).getProcessor(1);
    return new ImagePlus("Skeleton", skeleton);
  }

  private Edge closestEdge (Point p) {
    double bestDistance = Double.POSITIVE_INFINITY;
    Edge bestEdge = null;
    for (Graph graph : analyzeSkeleton.getGraphs()) {
      for (Edge edge : graph.getEdges()) {
        for (Point slab : edge.getSlabs()) {
          double dist = distance(p, slab);
          if (dist < bestDistance) {
            bestDistance = dist;
            bestEdge = edge;
          }
        }
      }
    }
    return bestEdge;
  }

  private Point convertPoint (java.awt.Point p) {
    return new Point(p.x, p.y, 0);
  }
}