package sc.fiji;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Developement>Visualize flow")
public class FlowVisualizer implements Command {

  /**
   * Parameters injected by ImageJ.
   */
  @Parameter
  private ImagePlus imp;

  private FloatProcessor gradientMagnitude;
  private FloatProcessor gradientDirection;

  /**
   * Main plugin method invoked by ImageJ.
   */
  @Override
  public void run () {
    computeGradients();
    ByteProcessor bp = imp.getProcessor().convertToByteProcessor();
    (new ImagePlus("AsBytes", bp)).show();
  }

  private void computeGradients () {
    int[] Gx = {
            3, 0, -3,
            10, 0, -10,
            3, 0, -3
    };
    int[] Gy = {
            3, 10, 3,
            0, 0, 0,
            -3, -10, -3
    };
    ImagePlus dup = imp.duplicate();
    IJ.run(dup, "Gaussian Blur...", "sigma=1");

    ImageProcessor Ipx = dup.getProcessor().duplicate();
    ImageProcessor Ipy = dup.getProcessor().duplicate();
    Ipx.convolve3x3(Gx);
    Ipy.convolve3x3(Gy);
    Ipx.sqr();
    Ipy.sqr();

    gradientMagnitude = new FloatProcessor(dup.getWidth(), dup.getHeight());
    gradientDirection = new FloatProcessor(dup.getWidth(), dup.getHeight());
    byte[] H = new byte[dup.getWidth() * dup.getHeight()];
    byte[] S = new byte[dup.getWidth() * dup.getHeight()];
    byte[] B = new byte[dup.getWidth() * dup.getHeight()];
    float maxMagnitude = 0;
    for (int x = 0; x < dup.getWidth(); x++) {
      for (int y = 0; y < dup.getHeight(); y++) {
        float magnitude = (float) Math.sqrt(Ipx.getf(x, y) + Ipy.getf(x, y));
        float direction = (float) Math.atan2(Ipy.getf(x, y), Ipx.getf(x, y));
        System.out.println(direction);
        gradientMagnitude.putPixelValue(x, y, magnitude);
        gradientDirection.putPixelValue(x, y, direction);
        H[y * dup.getWidth() + x] = (byte) Math.floor(((direction + Math.PI * 0.5f) / Math.PI) * 255);
        S[y * dup.getWidth() + x] = (byte) 0xff;
        if (maxMagnitude < magnitude) maxMagnitude = magnitude;
      }
    }

    for (int x = 0; x < dup.getWidth(); x++) {
      for (int y = 0; y < dup.getHeight(); y++) {
        float magnitude = gradientMagnitude.getf(x, y);
        B[y * dup.getWidth() + x] = (byte) Math.floor((magnitude / maxMagnitude)  * 255);
      }
    }

    ColorProcessor cpDirections = new ColorProcessor(dup.getWidth(), dup.getHeight());
    cpDirections.setHSB(H, S, B);
    (new ImagePlus("Gradients", cpDirections)).show();
    (new ImagePlus("gradientMagnitude", gradientMagnitude)).show();
    (new ImagePlus("gradientDirection", gradientDirection)).show();

  }
}