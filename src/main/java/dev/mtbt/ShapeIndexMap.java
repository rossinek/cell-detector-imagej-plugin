package dev.mtbt;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Calculate the shape index as defined in
 * J Koenderink and A van Doorn, “Surface shape and
 * curvature scales,” Image Vision Comput, vol. 10, no. 8,
 * pp. 557–565, 1992
 * https://github.com/fiji/Fiji_Plugins/blob/master/src/main/java/fiji/geom/Shape_Index_Map.java
 * @author Johannes Schindelin
 */
@Plugin(type = Command.class, menuPath = "Developement>Utils>Shape index map")
public class ShapeIndexMap implements Command {

  /**
   * Parameters injected by ImageJ.
   */
  @Parameter
  private ImagePlus imp;

  private static GaussianBlur gaussianBlur;

  /**
   * Main plugin method invoked by ImageJ.
   */
  @Override
  public void run () {
    final GenericDialog gd = new GenericDialog("Shape index map");
    gd.addNumericField("Gaussian_blur_radius (0 = off)", 0, 0);
    gd.showDialog();
    if (!gd.wasCanceled()) {
      getShapeIndexMap(imp, gd.getNextNumber()).show();
    }
  }

  public static ImagePlus getShapeIndexMap (final ImagePlus image, final double gaussianBlurRadius) {
    final ImageStack stack = image.getStack();
    final ImageStack result = new ImageStack(image.getWidth(), image.getHeight());
    for (int i = 1; i <= stack.getSize(); i++) {
      ImageProcessor ip = stack.getProcessor(i);
      if (gaussianBlurRadius > 0) {
        boolean isFloat = image.getType() != ImagePlus.GRAY32;
        final FloatProcessor fp = (FloatProcessor) (isFloat ? ip.convertToFloat() : ip.duplicate());
        if (gaussianBlur == null)
          gaussianBlur = new GaussianBlur();
        gaussianBlur.blurFloat(fp, gaussianBlurRadius, gaussianBlurRadius, 0.02);
        ip = fp;
      }
      result.addSlice("", getShapeIndex(ip));
    }
    return new ImagePlus("Shape index of " + image.getTitle(), result);
  }

  /**
   * The formula is:
   *
   * <pre>
   *                                 dnx_x + dny_y
   * s = 2 / PI * arctan ---------------------------------------
   *                     sqrt((dnx_x - dny_y)^2 + 4 dny_x dnx_y)
   * </pre>
   * <p>
   * where _x and _y are the x and y components of the partial derivatives of
   * the normal vector of the surface defined by the intensities of the image.
   * <p>
   * n_x and n_y are the negative partial derivatives of the intensity,
   * approximated by simple differences.
   *
   * @param ip the source image processor.
   * @return the shape index in a new image processor.
   */
  private static ImageProcessor getShapeIndex (final ImageProcessor ip) {
    final ImageProcessor dx = deriveX(ip);
    final ImageProcessor dy = deriveY(ip);
    final ImageProcessor dxx = deriveX(dx);
    final ImageProcessor dxy = deriveY(dx);
    final ImageProcessor dyx = deriveX(dy);
    final ImageProcessor dyy = deriveY(dy);

    final float factor = 2 / (float) Math.PI;
    final int w = ip.getWidth(), h = ip.getHeight();
    final FloatProcessor fp = new FloatProcessor(w, h);
    for (int i = 0; i < w; i++)
      for (int j = 0; j < h; j++) {
        final float dnx_x = -dxx.getf(i, j);
        final float dnx_y = -dxy.getf(i, j);
        final float dny_x = -dyx.getf(i, j);
        final float dny_y = -dyy.getf(i, j);
        final double D = Math.sqrt((dnx_x - dny_y) * (dnx_x - dny_y) + 4 * dnx_y * dny_x);
        final float s = factor * (float) Math.atan((dnx_x + dny_y) / D);
        fp.setf(i, j, Float.isNaN(s) ? 0 : s);
      }
    return fp;
  }

  private static ImageProcessor deriveX (final ImageProcessor ip) {
    final int w = ip.getWidth(), h = ip.getHeight();
    final FloatProcessor fp = new FloatProcessor(w, h);
    for (int j = 0; j < h; j++) {
      float previous = 0;
      for (int i = 0; i < w; i++) {
        final float current = ip.getf(i, j);
        fp.setf(i, j, current - previous);
        previous = current;
      }
    }
    return fp;
  }

  private static ImageProcessor deriveY (final ImageProcessor ip) {
    final int w = ip.getWidth(), h = ip.getHeight();
    final FloatProcessor fp = new FloatProcessor(w, h);
    for (int i = 0; i < w; i++) {
      float previous = 0;
      for (int j = 0; j < h; j++) {
        final float current = ip.getf(i, j);
        fp.setf(i, j, current - previous);
        previous = current;
      }
    }
    return fp;
  }
}
