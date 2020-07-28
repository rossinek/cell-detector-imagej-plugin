package dev.mtbt.imagej;

import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.plugin.SubHyperstackMaker;
import ij.process.ImageConverter;

public class HyperstackHelper {
  public static ImagePlus extractFrame(ImagePlus imp, int channel, int slice, int frame) {
    return SubHyperstackMaker.makeSubhyperstack(imp.duplicate(), Integer.toString(channel),
        Integer.toString(slice), Integer.toString(frame));
  }

  public static ImagePlus extractChannel(ImagePlus imp, int channel) {
    Duplicator duplicator = new Duplicator();
    return duplicator.run(imp, channel, channel, imp.getZ(), imp.getZ(), 1, imp.getNFrames());
  }

  public static ImagePlus extractGrayFrame(ImagePlus imp, int channel, int slice, int frame) {
    return extractGrayFrame(imp, channel, slice, frame, imp.getBitDepth());
  }

  public static ImagePlus extractGrayFrame(ImagePlus imp, int channel, int slice, int frame,
      int bitDepth) {
    ImagePlus output = extractFrame(imp, channel, slice, frame);
    ImageConverter converter = new ImageConverter(output);
    switch (bitDepth) {
      case 8:
        converter.convertToGray8();
        break;
      case 16:
        converter.convertToGray16();
        break;
      case 32:
        converter.convertToGray32();
        break;
      default:
        throw new IllegalArgumentException(
            "[HyperstackHelper] Can't find converter for provided input.");
    }

    return output;
  }
}
