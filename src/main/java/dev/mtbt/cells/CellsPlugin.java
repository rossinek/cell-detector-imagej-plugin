package dev.mtbt.cells;

import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

import java.util.List;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.InteractiveCommand;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;

import dev.mtbt.ImageJUtils;
import dev.mtbt.cells.skeleton.CellDetector;

@Plugin(type = Command.class, menuPath = "Developement>Main Plugin")
public class CellsPlugin extends InteractiveCommand implements Initializable {

  private List<Cell<ICellFrame>> cells;

  @Parameter
  private ImagePlus imp;

  @Parameter
  private UIService uiService;

  // DIALOG INPUTS
  @Parameter(label = "Select detector", choices = { "skeleton.CellDetector" })
  private String detector = null;

  @Parameter(label = "Run detector", callback = "onRunClick")
  private Button runButton;

  @Parameter(label = "Show!", callback = "onShowClick")
  private Button showButton;

  @Override
  public void initialize() {
    System.out.println("[CellsPlugin] initialize");
    if (imp == null)
      return;
  }

  @Override
  public void run() {
    System.out.println("[CellsPlugin] run");
  }

  @Override
  public void preview() {
    System.out.println("[CellsPlugin] preview");
  }

  protected void onRunClick() {
    if (detector == null) {
      uiService.showDialog("Select detector first");
      return;
    }
    Thread t = new Thread(() -> {
      try {
        System.out.println(">>> run <<<");
        final ModuleService moduleService = this.getContext().service(ModuleService.class);
        final CommandService commandService = this.getContext().service(CommandService.class);
        switch (detector) {
          case "skeleton.CellDetector":
            this.cells = ((CellDetector) moduleService.run(commandService.getCommand(CellDetector.class), true).get()).output().get();
            break;
          default:
            uiService.showDialog("No such detector");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    t.start();
  }

  protected void onShowClick() {
    this.displayCells();
  }

  private void displayCells() {
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    cells.forEach(cell -> roiManager.addRoi(cell.toRoi(this.imp.getFrame())));
    roiManager.runCommand("show all with labels");
  }
}
