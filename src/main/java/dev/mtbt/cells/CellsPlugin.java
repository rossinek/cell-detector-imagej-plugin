package dev.mtbt.cells;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
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

  private CellCollection cellCollection = new CellCollection();

  private RunnableCheckBox showEndpointsCheckBox;

  protected CellObserver cellObserver;

  @Override
  public void run() {
    if (imp == null)
      return;

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
    this.initializeCellObserver();
  }

  private void initializeCellObserver() {
    // final ImagePlus observedImage = this.impPreviewStack;
    final CellsPlugin context = this;

    this.cellObserver = new CellObserver(new CellObserverListener() {

      @Override
      public ImagePlus getObservedImage() {
        return context.impPreviewStack;
      }

      @Override
      public void cellFrameRoisShorten(ShapeRoi shapeRoi) {
        context.getCurrentCells().forEach(c -> c.cellFrameRoisShorten(shapeRoi));
      }

      @Override
      public void cellFrameRoisCut(Line lineRoi) {
        context.getCurrentCells().forEach(c -> c.cellFrameRoisCut(lineRoi));
      }

      @Override
      public void cellFrameRoiModified(Roi modifiedRoi) {
        context.getCurrentCells().forEach(c -> c.cellFrameRoiModified(modifiedRoi));
      }
    });
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
    this.cellCollection.destroy();
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.close();
    this.impPreviewStack.close();
  }

  protected List<Cell> getCurrentCells() {
    int frame = this.impPreviewStack.getT();
    return cellCollection.getCells(frame);
  }

  private void displayCells() {
    if (this.cellCollection == null)
      return;
    RoiManager roiManager = ImageJUtils.getRoiManager();
    roiManager.reset();
    roiManager.runCommand("show all with labels");
    roiManager.runCommand("usenames", "true");

    List<Cell> currentCells = getCurrentCells();
    int frame = this.impPreviewStack.getT();
    currentCells.stream().forEach(cell -> roiManager.addRoi(cell.getObservedRoi(frame)));
    if (this.showEndpointsCheckBox.isSelected()) {
      currentCells.stream()
          .forEach(cell -> cell.endsToRois(frame).forEach(roi -> roiManager.addRoi(roi)));
    }
  }

  @Override
  public void imageOpened(ImagePlus image) {
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
