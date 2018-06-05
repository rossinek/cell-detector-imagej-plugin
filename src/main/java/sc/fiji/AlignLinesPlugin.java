package sc.fiji;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

@Plugin(type = Command.class, menuPath = "Developement>Align lines")
public class AlignLinesPlugin implements Command {

	/**
	 * Parameters injected by ImageJ.
	 */
	@Parameter
	private UIService uiService;
	@Parameter
	private LogService logService;
	@Parameter
	private ImagePlus imp;

	/**
	 * Main plugin method invoked by ImageJ.
	 */
	@Override
	public void run () {
		this.alignLines();
	}

	private void validateRois (RoiManager roiManager) {
		if (roiManager == null) {
			throw new IllegalArgumentException("No active Roi Manager instance.");
		}
		if (roiManager.getCount() < 1) {
			throw new IllegalArgumentException("No selected ROIs.");
		}
		for (Roi roi : roiManager.getRoisAsArray()) {
			if (roi.getType() != Roi.POLYLINE) {
				throw new IllegalArgumentException("All ROIs should be a polyline.");
			}
		}
	}

	private void alignLines () {
		RoiManager roiManager = RoiManager.getInstance();
		try {
			//			validateRois(roiManager);
		} catch (IllegalArgumentException e) {
			uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
			return;
		}

		//		ImagePlus frame = HyperstackHelper.extractGray8Frame(imp, imp.getC(), imp.getSlice(), imp.getT());
		getBinaryPattern();
		//		for (Roi _roi : roiManager.getRoisAsArray()) {
		//			PolygonRoi roi = (PolygonRoi) _roi;
		//			// System.out.println(roi.getStrokeWidth());
		//			//			System.out.println("Score: " + computeLineScore(frame, roi));
		//		}
	}

	private ImagePlus getBinaryPattern () {
		ImagePlus frame = imp.duplicate();
		ImagePlus frame2 = imp.duplicate();
		// HyperstackHelper.extractGray16Frame(imp, imp.getC(), imp.getSlice(), imp.getT());
		//		RankFilters rankFilters = new RankFilters();
		//		rankFilters.rank(frame.getProcessor(), 2.0, RankFilters.VARIANCE);
		IJ.run(frame, "Gaussian Blur...", "sigma=2 stack");
		IJ.run(frame, "Variance...", "radius=2 stack");
		IJ.run(frame, "Convert to Mask", "method=Huang background=Dark calculate black");

		IJ.run(frame2, "Gaussian Blur...", "sigma=2 stack");
		IJ.run(frame2, "Variance...", "radius=2 stack");
		IJ.run(frame2, "Convert to Mask", "method=Triangle background=Dark calculate black");
		//		IJ.setAutoThreshold(frame, "Triangle dark");
		//		IJ.run(frame, "Convert to Mask", "");
		//		frame.updateAndDraw();
		//		frame.show();
		//				ContrastEnhancer contrastEnhancer = new ContrastEnhancer();
		//				contrastEnhancer.equalize(frame.getProcessor());
		//				contrastEnhancer.stretchHistogram(frame.getProcessor(), 0.3);
		//
		//		frame.getProcessor().setAutoThreshold("Triangle dark");

		//		frame.getProcessor().resetThreshold();
		//		double lower = frame.getProcessor().getMinThreshold();
		//		double upper = frame.getProcessor().getMaxThreshold();
		//		System.out.println("lower upper: " + lower + " " + upper);
		//		frame.getProcessor().setThreshold(lower, upper, ImageProcessor.BLACK_AND_WHITE_LUT);
		//		IJ.run(frame, "Convert to Mask", "");
		//		frame.getProcessor().resetThreshold();
		//		frame.getProcessor().autoThreshold();
		//		(new Thresholder()).run("mask");
		//		frame.show();
		//		ThresholdAdjuster.setMode("B&W");
		//		ThresholdAdjuster.setMethod("Shanbhag");

		//		Thresholder.setMethod("Shanbhag");
		//		Thresholder.setBackground("Dark");
		//		ThresholdAdjuster
		//		ThresholdAdjuster thresholdAdjuster = new ThresholdAdjuster();
		//		thresholdAdjuster.
		//		frame.getProcessor().setAutoThreshold("Shanbhag white");
		//		frame.getProcessor().autoThreshold();

		//		frame.getProcessor().threshold(frame.getProcessor().getAutoThreshold());
		//		frame.getProcessor().threshold(t);
		//		frame.getProcessor().threshold(arg0);
		//		frame.getProcessor().convertToByteProcessor()
		//				BinaryProcessor binaryProcessor = new BinaryProcessor(frame.getProcessor().convertToByteProcessor());
		//		ImagePlus binary = new ImagePlus("[binary] " + frame.getTitle(), binaryProcessor);
		System.out.println("Is binary: " + frame.getProcessor().isBinary());
		System.out.println("Is threshold: " + frame.isThreshold());

		//		//		frame.setProcessor(frame.getProcessor().crea);
		frame.show();
		frame2.show();
		//		float[] kernel = {
		//		    -1, -1, -1, -1, -1,
		//		    -1, -1, 24, -1, -1,
		//		    -1, -1, -1, -1, -1,
		//		};
		//		frame.getProcessor().convolve(kernel, 5, 5);
		//		frame.show();
		return frame;
	}

	private int computeLineScore (ImagePlus input, PolygonRoi roi) {
		input.show();
		return 0;
	}
}