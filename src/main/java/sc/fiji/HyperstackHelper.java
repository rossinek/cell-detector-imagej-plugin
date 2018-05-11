package sc.fiji;

import ij.ImagePlus;
import ij.plugin.SubHyperstackMaker;
import ij.process.ImageConverter;

public class HyperstackHelper {
	public static ImagePlus extractFrame (ImagePlus imp, int channel, int slice, int frame) {
		return SubHyperstackMaker.makeSubhyperstack(imp, Integer.toString(channel),
		    Integer.toString(slice), Integer.toString(frame));
	}

	public static ImagePlus extractGray8Frame (ImagePlus imp, int channel, int slice, int frame) {
		ImagePlus output = SubHyperstackMaker.makeSubhyperstack(imp, Integer.toString(channel),
		    Integer.toString(slice), Integer.toString(frame));
		ImageConverter converter = new ImageConverter(output);
		converter.convertToGray8();
		return output;
	}
}
