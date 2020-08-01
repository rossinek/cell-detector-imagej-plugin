package dev.mtbt.vendor;

import java.io.File;
import loci.plugins.BF;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import ij.ImagePlus;

@Plugin(type = Command.class, menuPath = "Mycobacterium>Utils>Open Bio Format")
public class BioFormatsRunner extends DynamicCommand {
  @Parameter
  private UIService uiService;

  @Override
  public void run() {
    final File file = uiService.chooseFile(null, "open");
    if (file != null) {
      try {
        ImagePlus[] imps;
        imps = BF.openImagePlus(file.getAbsolutePath());
        for (ImagePlus imp : imps) {
          uiService.show(imp);
        }
      } catch (Exception e) {
        uiService.showDialog("Ops, something went wrong...");
        e.printStackTrace();
      }
    }
  }
}
