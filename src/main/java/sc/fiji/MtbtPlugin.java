package sc.fiji;

import java.awt.Color;
import java.awt.Polygon;
import java.io.File;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import ij.blob.ManyBlobs;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import loci.plugins.BF;
import net.imagej.Dataset;
import net.imagej.ImageJ;

@Plugin(type = Command.class, menuPath = "Developement>MtbtPlugin")
public class MtbtPlugin extends ContextCommand {

	/*
	 * Parameters injected by ImageJ.
	 */
	@Parameter
	private LogService logService;
	@Parameter
	private Dataset dataset;
	@Parameter
	private ImagePlus imp;

	/*
	 * This is main plugin method invoked by ImageJ.
	 */
	@Override
	public void run () {
		System.out.println("channels: " + dataset.getChannels());
		System.out.println("frames: " + dataset.getFrames());
		System.out.println("(w, h): (" + dataset.getWidth() + ", " + dataset.getHeight() + ")");
		RoiManager roiManager = RoiManager.getInstance();
		if (roiManager == null) {
			roiManager = new RoiManager();
		}

		this.roisFromBlobs(roiManager);
	}

	private void addROIs (RoiManager roiManager) {
		PolygonRoi redPolygon = new PolygonRoi(
		    new float[] { 10, 100, 100, 400, 400, 10 },
		    new float[] { 10, 10, 200, 200, 300, 300 },
		    Roi.POLYGON);
		redPolygon.setStrokeColor(Color.RED);
		PolygonRoi greenPolygon = new PolygonRoi(
		    new float[] { 500, 600, 800, 800, 500 },
		    new float[] { 400, 500, 800, 1000, 1000 },
		    Roi.POLYGON);
		greenPolygon.setStrokeColor(Color.GREEN);

		roiManager.add(imp, redPolygon, 0);
		roiManager.add(imp, greenPolygon, 1);
		roiManager.runCommand("Show All");
	}

	/*
	 * example function - works on MK07_06_R3D.dv photo
	 */
	private void roisFromBlobs (RoiManager roiManager) {
		ImagePlus channelImp = new ImagePlus("channel 0", ChannelSplitter.getChannel(imp, 1));
		ImageConverter converter = new ImageConverter(channelImp);
		converter.convertToGray8();
		GaussianBlur gaussianBlur = new GaussianBlur();
		gaussianBlur.blurGaussian(channelImp.getProcessor(), 4.0);
		channelImp.getProcessor().threshold(40);
		// channelImp.show();
		ManyBlobs allBlobs = new ManyBlobs(channelImp);
		channelImp.getProcessor().invert();
		allBlobs.findConnectedComponents();
		for (int i = 0; i < allBlobs.size(); i++) {
			Polygon p = allBlobs.get(i).getOuterContour();
			int n = p.npoints;
			float[] xs = new float[p.npoints];
			float[] ys = new float[p.npoints];
			for (int j = 0; j < n; j++) {
				xs[j] = p.xpoints[j] + 0.5f;
				ys[j] = p.ypoints[j] + 0.5f;
			}
			Roi roi = new PolygonRoi(xs, ys, n, Roi.POLYGON);
			roi.setStrokeColor(Color.GREEN);
			roiManager.add(imp, (PolygonRoi) roi.clone(), i);
			roiManager.runCommand("Show All");
		}
	}

	/*
	 * This method is for development purposes only. It allows running plugin
	 * directly from IDE.
	 */
	public static void main (final String ...args) throws Exception {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		final File file = ij.ui().chooseFile(null, "open");
		if (file != null) {
			// Open file with Bio-Formats
			ImagePlus[] imps = BF.openImagePlus(file.getAbsolutePath());
			for (ImagePlus imp : imps) {
				ij.ui().show(imp);
			}
			ij.command().run(MtbtPlugin.class, true);
		}
	}
}