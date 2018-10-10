package dev.mtbt;

import java.awt.Color;
import java.awt.Polygon;
import java.util.ArrayList;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.NumberWidget;

import ij.ImagePlus;
import ij.blob.ManyBlobs;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

@Plugin(type = Command.class, menuPath = "Developement>Select blobs")
public class SelectBlobsPlugin implements Command, Previewable {

	/**
	 * Parameters injected by ImageJ.
	 */
	@Parameter
	private LogService logService;
	@Parameter
	private ImagePlus imp;

	/**
	 * Input parameters
	 */
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String headerThreshold = "Values for threshold";
	@Parameter(label = "Work on channel", callback = "thresholdChannelInputChanged", persist = false)
	private int thresholdChannelInput = 1;
	@Parameter(label = "Work on frame", callback = "thresholdFrameInputChanged", persist = false)
	private int thresholdFrameInput = 1;
	@Parameter(label = "Gaussian blur sigma", style = NumberWidget.SCROLL_BAR_STYLE, min = "1.1", max = "40.0", stepSize = "0.1")
	private double sigmaInput = 4.0;
	@Parameter(label = "Threshold", style = NumberWidget.SCROLL_BAR_STYLE, min = "1", max = "255", stepSize = "1")
	private int thresholdInput = 40;

	@Parameter(label = "Enable realtime preview", persist = false)
	private boolean previewInput = true;
	@Parameter(label = "Preview channel", callback = "previewChannelInputChanged", persist = false)
	private int previewChannelInput = 1;

	/**
	 * Main plugin method invoked by ImageJ after input provided.
	 */
	@Override
	public void run () {
		this.selectBlobs();
	}

	/**
	 * Method invoked after cancel click in input modal.
	 */
	@Override
	public void cancel () {
		System.out.println(">> cancel");
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager != null) {
			roiManager.deselect();
			roiManager.reset();
			roiManager.close();
		}
	}

	/**
	 * Method invoked after any change in input modal.
	 */
	@Override
	public void preview () {
		if (previewInput) {
			this.selectBlobs();
		}
	}

	protected void thresholdFrameInputChanged () {
		this.thresholdFrameInput = Math.max(1, Math.min(this.thresholdFrameInput, imp.getNFrames()));
	}

	protected void thresholdChannelInputChanged () {
		this.thresholdChannelInput = Math.max(1, Math.min(this.thresholdChannelInput, imp.getNChannels()));
	}

	protected void previewChannelInputChanged () {
		this.previewChannelInput = Math.max(1, Math.min(this.previewChannelInput, imp.getNChannels()));
	}

	private void selectBlobs () {
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null) {
			roiManager = new RoiManager();
		}
		ImagePlus binaryImp = blurAndThreshold(this.imp, this.thresholdChannelInput, this.thresholdFrameInput,
		    this.sigmaInput, this.thresholdInput);
		ArrayList<PolygonRoi> rois = blobsAsRois(binaryImp);

		this.imp.setC(this.previewChannelInput);
		this.imp.setT(this.thresholdFrameInput);

		this.setRois(roiManager, rois);
		roiManager.runCommand("Show All");
	}

	private void setRois (RoiManager roiManager, ArrayList<PolygonRoi> rois) {
		roiManager.deselect();
		roiManager.reset();
		for (int i = 0; i < rois.size(); i++) {
			roiManager.add(this.imp, rois.get(i), i);
		}
	}

	private ImagePlus blurAndThreshold (ImagePlus input, int channel, int frame, double sigma, int threshold) {
		ImagePlus output = HyperstackHelper.extractGrayFrame(input, channel, input.getSlice(), frame, 8);
		output.getProcessor().blurGaussian(sigma);
		output.getProcessor().threshold(threshold);
		output.getProcessor().invert();
		return output;
	}

	private ArrayList<PolygonRoi> blobsAsRois (ImagePlus binaryImp) {
		ManyBlobs allBlobs = new ManyBlobs(binaryImp);
		ArrayList<PolygonRoi> rois = new ArrayList<>();
		try {
			allBlobs.findConnectedComponents();
		} catch (Exception e) {
			// pass
		}
		for (int i = 0; i < allBlobs.size(); i++) {
			Polygon p = allBlobs.get(i).getOuterContour();
			int n = p.npoints;
			float[] xs = new float[p.npoints];
			float[] ys = new float[p.npoints];
			for (int j = 0; j < n; j++) {
				xs[j] = p.xpoints[j] + 0.5f;
				ys[j] = p.ypoints[j] + 0.5f;
			}
			PolygonRoi roi = new PolygonRoi(xs, ys, n, Roi.POLYGON);
			roi.setStrokeColor(Color.GREEN);
			rois.add(roi);
		}
		return rois;
	}
}