package dev.mtbt.cells;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import javafx.util.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Skeleton {
  private FloatProcessor initialImage;
  private ByteProcessor skeleton;
  ArrayList<Polygon> segments;

  public Skeleton (ImageProcessor ip) {
    initialImage = ip.convertToFloatProcessor();
    skeleton = ip.convertToByteProcessor();
    skeleton.threshold(1);
    skeleton.invert();
    skeleton.skeletonize();
    skeleton.invert();
  }

  public Skeleton (ImagePlus imp) {
    this(imp.getProcessor());
  }

  /**
   * Find finite poly line containing point
   *
   * @param point should lay on skeleton
   * @return poly line containing point
   */
  public Polygon findSpine (Point point) {
    Polygon polyLine = new Polygon();
    return polyLine;
  }

  public ImagePlus toImagePlus () {
    return new ImagePlus("Skeleton", skeleton);
  }

  // TODO: implement better algorithm for finding nearest point
  public Point closestPoint (Point p) {
    Point closestPoint = null;
    double bestDistance = Float.POSITIVE_INFINITY;
    int maxRadius = Math.max(skeleton.getWidth(), skeleton.getHeight());
    Point[] candidates = new Point[4];
    for (int radius = 1; radius < maxRadius; radius++) {
      int w = (2*radius) + 3;
      for (int i = -radius; i <= radius; i++) {
        candidates[0] = new Point(p.x + i, p.y - radius);
        candidates[1] = new Point(p.x + i, p.y + radius);
        candidates[2] = new Point(p.x - radius, p.y + i);
        candidates[3] = new Point(p.x + radius, p.y + i);
        for (Point c : candidates) {
          if (skeleton.getPixel(c.x, c.y) > 0 && distance(p, c) < bestDistance) {
            bestDistance = distance(p, c);
            closestPoint = c;
            maxRadius = Math.min(maxRadius, (int) Math.ceil(bestDistance));
          }
        }
      }
    }
    return closestPoint;
  }

  private double distance (Point p0, Point p1) {
    return Math.sqrt(((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y)));
  }
}