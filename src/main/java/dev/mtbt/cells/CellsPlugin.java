package dev.mtbt.cells;

import ij.ImageListener;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import dev.mtbt.ImageJUtils;
import dev.mtbt.cells.skeleton.SkeletonCellDetector;
import dev.mtbt.cells.skeleton.SkeletonCellLifeTracker;
import dev.mtbt.gui.DialogStepperActions;
import dev.mtbt.gui.RunnableCheckBox;
import dev.mtbt.gui.StackWindowWithPanel;

enum CellsPluginStepType {
  Detector, LifeTracker, Measurements;

  private static CellsPluginStepType[] vals = values();

  public CellsPluginStepType previous() {
    return vals[(vals.length + this.ordinal() - 1) % vals.length];
  }

  public CellsPluginStepType next() {
    return vals[(this.ordinal() + 1) % vals.length];
  }

  public boolean isFirst() {
    return this.ordinal() == 0;
  }

  public boolean isLast() {
    return this.ordinal() == vals.length - 1;
  }
}


@Plugin(type = Command.class, menuPath = "Development>Cell detector")
public class CellsPlugin extends DynamicCommand implements ImageListener {
  @Parameter
  private ImagePlus imp;
  @Parameter
  private UIService uiService;

  private CellsPluginStepType currentStep = CellsPluginStepType.Detector;
  private ICellsPluginStep currentStepInstance;

  protected ImagePlus impPreviewStack = null;
  protected StackWindowWithPanel dialog;
  protected JPanel dialogContent;
  protected CellsPluginToolbar toolbar;
  protected DialogStepperActions dialogActions;

  private CellCollection cellCollection;

  private RunnableCheckBox showEndpointsCheckBox;

  @Override
  public void run() {
    ImagePlus.addImageListener(this);
    if (imp == null)
      return;

    // JPanel cardDetection = new JPanel();
    // cardDetection.setLayout(new BoxLayout(cardDetection, BoxLayout.Y_AXIS));
    // detectorSelect = new JComboBox<>(detectorOptions);
    // detectorSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
    // detectorSelect.addActionListener(this);
    // cardDetection.add(detectorSelect);
    // RunnableButton detectorButton = new RunnableButton("Run detector", this::runDetector);
    // detectorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    // cardDetection.add(detectorButton);

    // JPanel cardLifeTracking = new JPanel();
    // cardLifeTracking.setLayout(new BoxLayout(cardLifeTracking, BoxLayout.Y_AXIS));
    // lifeTrackerSelect = new JComboBox<>(lifeTrackerOptions);
    // lifeTrackerSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
    // lifeTrackerSelect.addActionListener(this);
    // cardLifeTracking.add(lifeTrackerSelect);
    // RunnableButton lifeTrackerButton = new RunnableButton("Run life tracker",
    // this::runLifeTracker);
    // lifeTrackerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    // cardLifeTracking.add(lifeTrackerButton);

    // JPanel cardMeasurements = new JPanel();
    // cardMeasurements.setLayout(new BoxLayout(cardMeasurements, BoxLayout.Y_AXIS));
    // measurementsCellNameSelect = new JComboBox<>();
    // measurementsCellNameSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
    // cardMeasurements.add(measurementsCellNameSelect);
    // RunnableButton measureLengthsButton =
    // new RunnableButton("Measure lengths", this::showLengthsTable);
    // measureLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    // cardMeasurements.add(measureLengthsButton);
    // RunnableButton plotLengthsButton = new RunnableButton("Plot lengths", this::plotLengths);
    // plotLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    // cardMeasurements.add(plotLengthsButton);

    // RunnableButton plotProfileButton = new RunnableButton("Plot profile", this::plotProfile);
    // plotLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    // cardMeasurements.add(plotProfileButton);

    // this.dialog =
    // new DialogStepper("Main plugin window", this.createSettingsComponent(), this::cleanup);
    // this.dialog.registerStep(new DialogStepperStep(dialog, "Detection", cardDetection));
    // this.dialog.registerStep(new DialogStepperStep(dialog, "LifeTracking", cardLifeTracking));
    // this.dialog.registerStep(new DialogStepperStep(dialog, "Measurements", cardMeasurements, ()
    // -> {
    // List<String> cells = CellAnalyzer.getAllGenerationsNames(this.cellCollection);
    // this.measurementsCellNameSelect
    // .setModel(new DefaultComboBoxModel<>(cells.toArray(new String[cells.size()])));
    // }));
    // this.dialog.setVisible(true);

    this.cellCollection = new CellCollection();
    this.impPreviewStack = this.imp.duplicate();

    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.add(Box.createVerticalStrut(20));

    this.dialogContent = new JPanel();
    this.dialogContent.setLayout(new BoxLayout(this.dialogContent, BoxLayout.Y_AXIS));
    addCenteredComponent(contentPanel, this.dialogContent);

    JPanel footerPanel = new JPanel();
    footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
    this.toolbar = new CellsPluginToolbar();
    this.dialogActions =
        new DialogStepperActions(this::previousStep, this::nextStep, this::cleanup);
    addCenteredComponent(footerPanel, this.toolbar);
    addCenteredComponent(footerPanel, createSettingsComponent());
    addCenteredComponent(footerPanel, this.dialogActions);

    this.dialog = new StackWindowWithPanel(this.impPreviewStack);
    this.dialog.getSidePanel().add(contentPanel, BorderLayout.NORTH);
    this.dialog.getSidePanel().add(footerPanel, BorderLayout.SOUTH);
    this.updateStep();
    this.dialog.pack();
  }

  private void previousStep() {
    if (!this.currentStep.isFirst()) {
      this.currentStep = this.currentStep.previous();
      this.dialogActions.setIsFirst(this.currentStep.isFirst());
      this.dialogActions.setIsLast(this.currentStep.isLast());
      this.updateStep();
    }
  }

  private void nextStep() {
    if (!this.currentStep.isLast()) {
      this.currentStep = this.currentStep.next();
      this.dialogActions.setIsFirst(this.currentStep.isFirst());
      this.dialogActions.setIsLast(this.currentStep.isLast());
      this.updateStep();
    }
  }

  private void updateStep() {
    if (this.currentStepInstance != null) {
      this.currentStepInstance.cleanup();
    }
    switch (this.currentStep) {
      case Detector:
        this.currentStepInstance = new SkeletonCellDetector();
        break;
      case LifeTracker:
        this.currentStepInstance = new SkeletonCellLifeTracker();
        break;
      case Measurements:
        this.currentStepInstance = new StepMeasurements();
        break;
    }
    this.dialogContent.removeAll();
    JComponent content = this.currentStepInstance.init(this.impPreviewStack, this.cellCollection);
    this.dialogContent.add(content);
    this.dialog.pack();
  }

  protected void addCenteredComponent(JPanel panel, JComponent component) {
    component.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(component);
  }

  private JComponent createSettingsComponent() {
    Box settingsBox = new Box(BoxLayout.X_AXIS);
    this.showEndpointsCheckBox = new RunnableCheckBox("show endpoints", this::preview);
    settingsBox.add(showEndpointsCheckBox);
    return settingsBox;
  }

  public void preview() {
    if (this.impPreviewStack == null)
      return;
    this.displayCells();
    this.impPreviewStack.updateAndDraw();
  }

  private void cleanup() {
    ImagePlus.removeImageListener(this);
    if (this.cellCollection == null)
      return;
    this.cellCollection.destroy();
    this.cellCollection = null;
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
  }

  protected void showFirstFrameWithCells() {
    int frame = this.cellCollection.isEmpty() ? 1 : this.cellCollection.getF0();
    this.impPreviewStack.setT(frame);
  }

  private void displayCells() {
    if (this.cellCollection == null)
      return;
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.runCommand("show all with labels");
    roiManager.runCommand("usenames", "true");
    int frame = this.impPreviewStack.getFrame();
    List<Cell> currentCells = cellCollection.getCells(frame);
    currentCells.stream().forEach(cell -> roiManager.addRoi(cell.getObservedRoi(frame)));
    if (this.showEndpointsCheckBox.isSelected()) {
      currentCells.stream()
          .forEach(cell -> cell.endsToRois(frame).forEach(roi -> roiManager.addRoi(roi)));
    }

  }

  @Override
  public void imageOpened(ImagePlus image) {
    // Ignore
  }

  @Override
  public void imageClosed(ImagePlus image) {
    if (image == this.impPreviewStack && this.cellCollection != null) {
      this.cellCollection.destroy();
    }
  }

  @Override
  public void imageUpdated(ImagePlus image) {
    if (image == this.impPreviewStack) {
      this.displayCells();
    }
  }
}
