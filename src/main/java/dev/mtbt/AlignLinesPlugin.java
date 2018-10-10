package dev.mtbt;

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
		ImagePlus frame = HyperstackHelper.extractChannel(imp, imp.getChannel());
		IJ.run(frame, "Gaussian Blur...", "sigma=2 stack");
		IJ.run(frame, "Variance...", "radius=2 stack");
		IJ.run(frame, "Enhance Contrast...", "saturated=0.3 equalize process_all use");
		IJ.run(frame, "Convert to Mask", "method=Otsu background=Dark calculate black");
		System.out.println("Is binary: " + frame.getProcessor().isBinary());
		System.out.println("Is threshold: " + frame.isThreshold());

		frame.show();
		return frame;
	}

	private int computeLineScore (ImagePlus input, PolygonRoi roi) {
		input.show();
		return 0;
	}
}