package dev.mtbt.cells;

import ij.ImageListener;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import dev.mtbt.cells.skeleton.SkeletonCellDetector;
import dev.mtbt.cells.skeleton.SkeletonCellLifeTracker;
import dev.mtbt.gui.DialogStepperActions;
import dev.mtbt.gui.RunnableCheckBox;
import dev.mtbt.gui.StackWindowWithPanel;

@Plugin(type = Command.class, menuPath = "Development>Cell detector")
public class CellsPlugin extends DynamicCommand implements ImageListener {
  public static CellsPlugin instance;

  @Parameter
  private ImagePlus imp;

  private CellsPluginStepType currentStep = CellsPluginStepType.Detector;
  private ICellsPluginStep currentStepInstance;

  protected ImagePlus impPreviewStack = null;
  protected StackWindowWithPanel dialog;
  protected JPanel dialogContent;
  protected CellsPluginToolbar toolbar;
  protected DialogStepperActions dialogActions;

  private CellCollection cellCollection = new CellCollection();

  private RunnableCheckBox showEndpointsCheckBox;

  protected CellsManager cellsManager;

  @Override
  public void run() {
    if (imp == null)
      return;

    CellsPlugin.instance = this;

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

    ImagePlus.addImageListener(this);
    this.cellsManager = new CellsManager(this.impPreviewStack, this.cellCollection);
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
    this.impPreviewStack.updateAndDraw();
  }

  public void cleanup() {
    CellsPlugin.instance = null;
    ImagePlus.removeImageListener(this);
    this.cellCollection.destroy();
    RoiManager roiManager = this.cellsManager.getObservedRoiManager();
    roiManager.reset();
    roiManager.close();
    if (this.currentStepInstance != null) {
      this.currentStepInstance.cleanup();
    }
    this.impPreviewStack.close();
  }

  @Override
  public void imageOpened(ImagePlus image) {
  }

  @Override
  public void imageClosed(ImagePlus image) {
    if (image == this.impPreviewStack) {
      this.cleanup();
    }
  }

  @Override
  public void imageUpdated(ImagePlus image) {
    if (image == this.impPreviewStack) {
      this.dialog.requestFocus();
      this.cellsManager.displayCells(this.showEndpointsCheckBox.isSelected());
      this.currentStepInstance.imageUpdated();
    }
  }

  public CellCollection getCellCollection() {
    return this.cellCollection;
  }

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
}
