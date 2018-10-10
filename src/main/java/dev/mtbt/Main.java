package dev.mtbt;

import java.io.File;

import ij.ImagePlus;
import loci.plugins.BF;
import net.imagej.ImageJ;

/**
 * This class is for development purposes only.
 * It allows running ImageJ with installed plugins directly from IDE.
 */
public class Main {
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
			// ij.command().run(SelectBlobsPlugin.class, true);
		}
	}

}
