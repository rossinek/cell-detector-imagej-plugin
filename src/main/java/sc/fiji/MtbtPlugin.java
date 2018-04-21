package sc.fiji;

import net.imagej.Dataset;
import net.imagej.ImageJ;

import java.io.File;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import loci.plugins.BF;

@Plugin(type = Command.class, menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "Developement", mnemonic = 'd'),
		@Menu(label = "MtbtPlugin", weight = 1) }, headless = true, attrs = { @Attr(name = "no-legacy") })
public class MtbtPlugin extends ContextCommand {

	/*
	 * Parameters injected by ImageJ.
	 */
	@Parameter
	private LogService logService;
	@Parameter
	private Dataset dataset;

	/*
	 * This is main plugin method invoked by ImageJ.
	 */
	@Override
	public void run() {
		System.out.println("channels: " + dataset.getChannels());
		System.out.println("frames: " + dataset.getFrames());
		System.out.println("(w, h): (" + dataset.getWidth() + ", " + dataset.getHeight() + ")");
	}

	/*
	 * This method is for development purposes only.
	 * It allows running plugin directly from IDE.
	 */
	public static void main(final String... args) throws Exception {
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