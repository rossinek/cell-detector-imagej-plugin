package dev.mtbt.cells;

import ij.ImageListener;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import dev.mtbt.ImageJUtils;
import dev.mtbt.cells.skeleton.SkeletonCellDetector;
import dev.mtbt.cells.skeleton.SkeletonCellLifeTracker;
import dev.mtbt.gui.DialogStepper;
import dev.mtbt.gui.DialogStepperStep;
import dev.mtbt.gui.RunnableButton;
import dev.mtbt.gui.RunnableCheckBox;

@Plugin(type = Command.class, menuPath = "Development>Cell detector")
public class CellsPlugin extends DynamicCommand implements ImageListener, ActionListener {
  @Parameter
  private ImagePlus imp;
  @Parameter
  private UIService uiService;

  private DialogStepper dialog;
  private CellCollection cellCollection;

  private JComboBox<String> detectorSelect;
  final String[] detectorOptions = {"SkeletonCellDetector"};
  private String detector = detectorOptions[0];

  private JComboBox<String> lifeTrackerSelect;
  final String[] lifeTrackerOptions = {"SkeletonCellLifeTracker"};
  private String lifeTracker = lifeTrackerOptions[0];

  private RunnableCheckBox showEndpointsCheckBox;

  @Override
  public void run() {
    ImagePlus.addImageListener(this);
    if (imp == null)
      return;

    JPanel cardDetection = new JPanel();
    cardDetection.setLayout(new BoxLayout(cardDetection, BoxLayout.Y_AXIS));
    detectorSelect = new JComboBox<>(detectorOptions);
    detectorSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
    detectorSelect.addActionListener(this);
    cardDetection.add(detectorSelect);
    RunnableButton detectorButton = new RunnableButton("Run detector", this::runDetector);
    detectorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    cardDetection.add(detectorButton);

    JPanel cardLifeTracking = new JPanel();
    cardLifeTracking.setLayout(new BoxLayout(cardLifeTracking, BoxLayout.Y_AXIS));
    lifeTrackerSelect = new JComboBox<>(lifeTrackerOptions);
    lifeTrackerSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
    lifeTrackerSelect.addActionListener(this);
    cardLifeTracking.add(lifeTrackerSelect);
    RunnableButton lifeTrackerButton = new RunnableButton("Run life tracker", this::runLifeTracker);
    lifeTrackerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    cardLifeTracking.add(lifeTrackerButton);

    this.dialog =
        new DialogStepper("Main plugin window", this.createSettingsComponent(), this::cleanup);
    this.dialog.registerStep(new DialogStepperStep(dialog, "Detection", cardDetection));
    this.dialog.registerStep(new DialogStepperStep(dialog, "LifeTracking", cardLifeTracking));
    this.dialog.setVisible(true);
  }

  private Component createSettingsComponent() {
    Box settingsBox = new Box(BoxLayout.X_AXIS);
    this.showEndpointsCheckBox = new RunnableCheckBox("show endpoints", this::preview);
    settingsBox.add(showEndpointsCheckBox);
    return settingsBox;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == this.detectorSelect) {
      this.detector = (String) this.detectorSelect.getSelectedItem();
    } else if (source == this.lifeTrackerSelect) {
      this.lifeTracker = (String) this.lifeTrackerSelect.getSelectedItem();
    }
  }

  public void preview() {
    this.displayCells();
    this.imp.updateAndDraw();
  }

  private void cleanup() {
    ImagePlus.removeImageListener(this);
    if (this.cellCollection == null)
      return;
    this.cellCollection = null;
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
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
        ICellDetector cellDetector;
        switch (detector) {
          case "SkeletonCellDetector":
            cellDetector =
                (ICellDetector) ms.run(cs.getCommand(SkeletonCellDetector.class), true).get();
            break;
          default:
            uiService.showDialog("No such detector");
            return;
        }
        this.cellCollection = cellDetector.output().get();
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
        ICellLifeTracker cellLifeTracker;
        switch (lifeTracker) {
          case "SkeletonCellLifeTracker":
            cellLifeTracker =
                (ICellLifeTracker) ms.run(cs.getCommand(SkeletonCellLifeTracker.class), true).get();
            break;
          default:
            uiService.showDialog("No such life tracker");
            return;
        }
        cellLifeTracker.init(this.cellCollection);
        cellLifeTracker.output().get();
        this.onLifeTrackingEnd();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    t.start();
  }

  protected void showFirstFrameWithCells() {
    int frame = this.cellCollection.isEmpty() ? 1 : this.cellCollection.getF0();
    this.imp.setT(frame);
  }

  protected void onDetectionEnd() {
    this.dialog.getCurrentStep().setFinished(true);
    this.showFirstFrameWithCells();
    this.preview();
  }

  protected void onLifeTrackingEnd() {
    this.dialog.getCurrentStep().setFinished(true);
    this.showFirstFrameWithCells();
    this.preview();
  }

  private void displayCells() {
    if (this.cellCollection == null)
      return;
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.runCommand("show all with labels");
    roiManager.runCommand("usenames", "true");
    int frame = this.imp.getFrame();
    List<Cell> currentCells = cellCollection.getCells(frame);
    currentCells.stream().forEach(cell -> roiManager.addRoi(cell.getObservedRoi(frame)));
    if (this.showEndpointsCheckBox.isSelected()) {
      currentCells.stream()
          .forEach(cell -> cell.endsToRois(frame).forEach(roi -> roiManager.addRoi(roi)));
    }

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
      this.displayCells();
    }
  }
}
