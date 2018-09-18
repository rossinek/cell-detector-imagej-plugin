package sc.fiji;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;

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
import ij.plugin.Selection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

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
			validateRois(roiManager);
		} catch (IllegalArgumentException e) {
			uiService.showDialog(e.getMessage(), DialogPrompt.MessageType.ERROR_MESSAGE);
			return;
		}

		ImagePlus binaryRef = getBinaryPattern();

		int[][] moves = {
		    { 0, 1 },
		    { 1, 1 },
		    { 1, 0 },
		    { 1, -1 },
		    { 0, -1 },
		    { -1, -1 },
		    { -1, 0 },
		    { -1, 1 },
		};

		for (Roi _roi : roiManager.getRoisAsArray()) {
			PolygonRoi basePolylineRoi = (PolygonRoi) _roi;

			PolygonRoi bestPolylineRoi = basePolylineRoi;
			Polygon polyline = basePolylineRoi.getPolygon();
			Polygon bestPolyline = polyline;
			double bestScore = this.computeLineScore(binaryRef.getProcessor(), bestPolylineRoi);
			for (int step = 0; step < 1000; step++) {
				for (int coord = 0; coord < bestPolyline.npoints; coord++) {
					for (int move = 0; move < moves.length; move++) {
						int[] xpoints = new int[bestPolyline.npoints];
						int[] ypoints = new int[bestPolyline.npoints];
						System.arraycopy(bestPolyline.xpoints, 0, xpoints, 0, bestPolyline.npoints);
						System.arraycopy(bestPolyline.ypoints, 0, ypoints, 0, bestPolyline.npoints);

						xpoints[coord] = xpoints[coord] + moves[move][0];
						ypoints[coord] = ypoints[coord] + moves[move][1];
						Polygon currentPolyline = new Polygon(xpoints, ypoints, bestPolyline.npoints);
						PolygonRoi currentPolylineRoi = new PolygonRoi(currentPolyline, Roi.POLYLINE);
						currentPolylineRoi.setStrokeWidth(basePolylineRoi.getStrokeWidth());
						double score = this.computeLineScore(binaryRef.getProcessor(), currentPolylineRoi);
						if (score > bestScore) {
							System.out.println("better! " + score + " > " + bestScore);
							bestPolyline = currentPolyline;
							bestScore = score;
							bestPolylineRoi = currentPolylineRoi;
						}
					}
				}
			}

			Roi strokeRoi = Selection.lineToArea(bestPolylineRoi);
			Rectangle bounds = strokeRoi.getBounds();
			ImageProcessor ip = drawPatch(bounds, strokeRoi);
			ImagePlus xxx = new ImagePlus("xxx", ip);
			xxx.show();
			xxx.updateAndDraw();

			bestPolylineRoi.setStrokeColor(Color.RED);
			bestPolylineRoi.setFillColor(Color.RED);
			roiManager.addRoi(bestPolylineRoi);
			imp.updateAndDraw();
		}
	}

	private ImageProcessor drawPatch (Rectangle bounds, Roi strokeRoi) {
		int strokeWidth = 3;
		int searchRadius = 10;

		ImageProcessor ip = new ByteProcessor((int) bounds.getWidth() + 2 * searchRadius,
		    (int) bounds.getHeight() + 2 * searchRadius);
		ip.setColor(Color.WHITE);
		ip.setLineWidth(strokeWidth);
		strokeRoi.setLocation(searchRadius, searchRadius);
		strokeRoi.drawPixels(ip);
		return ip;
	}

	private ImagePlus getBinaryPattern () {
		ImagePlus binary = HyperstackHelper.extractChannel(imp, imp.getChannel());
		IJ.run(binary, "Gaussian Blur...", "sigma=2 stack");
		IJ.run(binary, "Variance...", "radius=2 stack");
		IJ.run(binary, "Enhance Contrast...", "saturated=0.3 equalize process_all use");
		IJ.run(binary, "Convert to Mask", "method=Otsu background=Dark calculate black");
		binary.show();
		return binary;
	}

	private double computeLineScore (ImageProcessor binaryRef, PolygonRoi basePolyline) {
		Roi strokeRoi = Selection.lineToArea(basePolyline);
		int strokeWidth = 3;
		int searchRadius = 10;
		Rectangle bounds = strokeRoi.getBounds();
		ImageProcessor ip = drawPatch(bounds, strokeRoi);
		int offsetX = (int) (bounds.getX()) - searchRadius;
		int offsetY = (int) (bounds.getY()) - searchRadius;
		long intersection = 0;
		long strokeTotal = 0;
		int refVal, strokeVal;
		for (int y = 0; y < ip.getHeight(); y++) {
			for (int x = 0; x < ip.getWidth(); x++) {
				refVal = binaryRef.getPixel(x + offsetX, y + offsetY);
				strokeVal = ip.getPixel(x, y);
				if (strokeVal > 0) {
					strokeTotal++;
					if (refVal > 0) {
						intersection++;
					}
				}
			}
		}
		double intersectionScore = intersection / (double) strokeTotal;
		double baseLengthScore = (intersection / (double) strokeWidth) / basePolyline.getLength();
		return intersectionScore + baseLengthScore;
	}
}