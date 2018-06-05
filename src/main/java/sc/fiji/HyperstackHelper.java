package sc.fiji;

import ij.ImagePlus;
import ij.plugin.SubHyperstackMaker;
import ij.process.ImageConverter;

public class HyperstackHelper {
	public static ImagePlus extractFrame (ImagePlus imp, int channel, int slice, int frame) {
		return SubHyperstackMaker.makeSubhyperstack(imp, Integer.toString(channel),
		    Integer.toString(slice), Integer.toString(frame)).duplicate();
	}

	public static ImagePlus extractGray8Frame (ImagePlus imp, int channel, int slice, int frame) {
		ImagePlus output = extractFrame(imp, channel, slice, frame);
		ImageConverter converter = new ImageConverter(output);
		converter.convertToGray8();
		return output;
	}

	public static ImagePlus extractGray16Frame (ImagePlus imp, int channel, int slice, int frame) {
		ImagePlus output = extractFrame(imp, channel, slice, frame);
		ImageConverter converter = new ImageConverter(output);
		converter.convertToGray16();
		return output;
	}
}
