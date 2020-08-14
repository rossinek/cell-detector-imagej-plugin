package dev.mtbt.vendor.stabilizer;

import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Mycobacterium>Utils>Image Stabilizer")
public class ImageStabilizerRunner extends DynamicCommand {
  @Parameter
  private ImagePlus imp;

  @Override
  public void run() {
    Image_Stabilizer iStabilizer = new Image_Stabilizer();
    iStabilizer.setup("", imp);
    iStabilizer.run(imp.getProcessor());
  }
}
