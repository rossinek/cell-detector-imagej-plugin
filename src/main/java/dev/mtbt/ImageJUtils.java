package dev.mtbt;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.module.MethodCallException;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import ij.plugin.frame.RoiManager;

public class ImageJUtils {
  public static RoiManager getRoiManager() {
    RoiManager roiManager = RoiManager.getInstance();
    return roiManager == null ? new RoiManager() : roiManager;
  }

  public static <C extends Command> Module executeCommand(ModuleService moduleService,
      CommandService cmdService, final Class<C> cmdClass) {
    final Module module = moduleService.createModule(cmdService.getCommand(cmdClass));
    try {
      module.initialize();
    } catch (final MethodCallException ex) {
      ex.printStackTrace();
    }
    cmdService.run(cmdClass, true);
    module.run();
    final Future<Module> run = moduleService.run(module, true);
    try {
      run.get();
    } catch (final InterruptedException ex) {
      ex.printStackTrace();
    } catch (final ExecutionException ex) {
      ex.printStackTrace();
    }
    return module;
  }
}
