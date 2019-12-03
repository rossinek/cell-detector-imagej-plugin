package dev.mtbt.cells;

import ij.ImageListener;
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
import dev.mtbt.cells.skeleton.SkeletonCellDetector;
import dev.mtbt.cells.skeleton.SkeletonCellLifeTracker;

@Plugin(type = Command.class, menuPath = "Developement>Main Plugin")
public class CellsPlugin extends InteractiveCommand implements Initializable, ImageListener {
  @Parameter
  private ImagePlus imp;

  @Parameter
  private UIService uiService;

  @Parameter(label = "Select detector", choices = {"SkeletonCellDetector"})
  private String detector = null;

  @Parameter(label = "Run detector", callback = "runDetector")
  private Button runDetectorButton;

  @Parameter(label = "Select life tracker", choices = {"SkeletonCellLifeTracker"})
  private String lifeTracker = null;

  @Parameter(label = "Run life tracker", callback = "runLifeTracker")
  private Button runLifeTrackerButton;

  private PluginState state = PluginState.Detection;

  private List<Cell> cells;

  @Override
  public void initialize() {
    ImagePlus.addImageListener(this);
    if (imp == null)
      return;
  }

  @Override
  public void run() {
  }

  @Override
  public void preview() {
    switch (this.state) {
      case Detection:
        break;
      case LifeTracking:
      case Idle:
        this.displayCells();
        break;
    }
  }

  protected void runDetector() {
    if (detector == null) {
      uiService.showDialog("Select detector first");
      return;
    }
    Thread t = new Thread(() -> {
      try {
        final ModuleService ms = this.getContext().service(ModuleService.class);
        final CommandService cs = this.getContext().service(CommandService.class);
        CellDetector cellDetector;
        switch (detector) {
          case "SkeletonCellDetector":
            cellDetector =
                (CellDetector) ms.run(cs.getCommand(SkeletonCellDetector.class), true).get();
            break;
          default:
            uiService.showDialog("No such detector");
            return;
        }
        this.cells = cellDetector.output().get();
        this.onDetectionEnd();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    t.start();
  }

  protected void runLifeTracker() {
    if (lifeTracker == null) {
      uiService.showDialog("Select life tracker first");
      return;
    }
    Thread t = new Thread(() -> {
      try {
        final ModuleService ms = this.getContext().service(ModuleService.class);
        final CommandService cs = this.getContext().service(CommandService.class);
        CellLifeTracker cellLifeTracker;
        switch (lifeTracker) {
          case "SkeletonCellLifeTracker":
            cellLifeTracker =
                (CellLifeTracker) ms.run(cs.getCommand(SkeletonCellLifeTracker.class), true).get();
            break;
          default:
            uiService.showDialog("No such life tracker");
            return;
        }
        cellLifeTracker.init(this.cells);
        cellLifeTracker.output().get();
        this.onLifeTrackingEnd();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    t.start();
  }

  protected void onDetectionEnd() {
    if (this.state != PluginState.Detection)
      return;
    this.state = PluginState.LifeTracking;
    this.displayCells();
  }

  protected void onLifeTrackingEnd() {
    if (this.state != PluginState.LifeTracking)
      return;
    this.state = PluginState.Idle;
    this.displayCells();
  }

  private void displayCells() {
    if (this.cells == null)
      return;
    // this.imp.show();
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    int frame = this.imp.getFrame();
    Cell.evoluate(this.cells, frame).stream().map(cell -> cell.toRoi(frame))
        .filter(roi -> roi != null).forEach(roi -> roiManager.addRoi(roi));
    roiManager.runCommand("show all with labels");
  }

  @Override
  public void imageOpened(ImagePlus imp) {
    // Ignore
  }

  @Override
  public void imageClosed(ImagePlus imp) {
    // Ignore
  }

  @Override
  public void imageUpdated(ImagePlus image) {
    if (image == this.imp) {
      this.preview();
    }
  }

  public enum PluginState {
    Detection, LifeTracking, Idle
  }
}
