package dev.mtbt.cells;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.ProfilePlot;
import ij.measure.ResultsTable;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.frame.RoiManager;
import java.awt.Component;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import dev.mtbt.HyperstackHelper;
import dev.mtbt.ImageJUtils;
import dev.mtbt.cells.skeleton.SkeletonCellDetector;
import dev.mtbt.cells.skeleton.SkeletonCellLifeTracker;
import dev.mtbt.gui.DialogStepper;
import dev.mtbt.gui.DialogStepperStep;
import dev.mtbt.gui.RunnableButton;
import dev.mtbt.gui.RunnableCheckBox;
import dev.mtbt.util.PicksFinder;

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

  private JComboBox<String> measurementsCellNameSelect;

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

    JPanel cardMeasurements = new JPanel();
    cardMeasurements.setLayout(new BoxLayout(cardMeasurements, BoxLayout.Y_AXIS));
    measurementsCellNameSelect = new JComboBox<>();
    measurementsCellNameSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
    cardMeasurements.add(measurementsCellNameSelect);
    RunnableButton measureLengthsButton =
        new RunnableButton("Measure lengths", this::showLengthsTable);
    measureLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    cardMeasurements.add(measureLengthsButton);
    RunnableButton plotLengthsButton = new RunnableButton("Plot lengths", this::plotLengths);
    plotLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    cardMeasurements.add(plotLengthsButton);

    RunnableButton plotProfileButton = new RunnableButton("Plot profile", this::plotProfile);
    plotLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    cardMeasurements.add(plotProfileButton);

    this.dialog =
        new DialogStepper("Main plugin window", this.createSettingsComponent(), this::cleanup);
    this.dialog.registerStep(new DialogStepperStep(dialog, "Detection", cardDetection));
    this.dialog.registerStep(new DialogStepperStep(dialog, "LifeTracking", cardLifeTracking));
    this.dialog.registerStep(new DialogStepperStep(dialog, "Measurements", cardMeasurements, () -> {
      List<String> cells = CellAnalyzer.getAllGenerationsNames(this.cellCollection);
      this.measurementsCellNameSelect
          .setModel(new DefaultComboBoxModel<>(cells.toArray(new String[cells.size()])));
    }));
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
    this.cellCollection.destroy();
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
        if (this.cellCollection != null) {
          this.cellCollection.destroy();
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

  protected void showLengthsTable() {
    String cellName = (String) this.measurementsCellNameSelect.getSelectedItem();
    Cell cell = CellAnalyzer.getCellByName(this.cellCollection, cellName);
    this.measureLengths(cell).show("Lengths table");
  }

  protected ResultsTable measureLengths(Cell cell) {
    ResultsTable table = new ResultsTable();
    for (CellFrame frame : cell.getFrames()) {
      table.incrementCounter();
      table.addValue("Length", frame.getLength());
    }
    return table;
  }

  protected void plotLengths() {
    String cellName = (String) this.measurementsCellNameSelect.getSelectedItem();
    Cell cell = CellAnalyzer.getCellByName(this.cellCollection, cellName);
    ResultsTable table = this.measureLengths(cell);

    table.getColumnAsDoubles(0);
    double[] frames =
        IntStream.range(cell.getF0(), cell.getFN()).mapToDouble(i -> (double) i).toArray();
    Plot plot = new Plot("Length plot", "Frame index", "Length");
    plot.add("line", frames, table.getColumnAsDoubles(0));
    plot.show();
  }

  protected void plotProfile() {
    String cellName = (String) this.measurementsCellNameSelect.getSelectedItem();
    Cell cell = CellAnalyzer.getCellByName(this.cellCollection, cellName);
    Plot plot = new Plot("Profile maxima in time", "Frame index", "Position on the body (0-1)");
    plot.setColor(Color.BLACK);
    plot.addLabel(0.001, 0, "     low intensity");
    plot.setColor(Color.getHSBColor(0.16f, 1f, 0.9f));
    plot.addLabel(0, 0, "■");
    plot.setColor(Color.BLACK);
    plot.addLabel(0.251, 0, "     high intensity");
    plot.setColor(Color.RED);
    plot.addLabel(0.25, 0, "■");
    int f0 = cell.getF0();
    int fN = cell.getFN();
    int nFrames = fN - f0;
    Plot[] picksPlots = new Plot[nFrames];
    float maxValue = Float.NEGATIVE_INFINITY;
    float minValue = Float.POSITIVE_INFINITY;
    for (int frame = f0; frame < fN; frame++) {
      ImagePlus frameImp =
          HyperstackHelper.extractFrame(imp, imp.getChannel(), imp.getSlice(), frame);
      new GaussianBlur().blurGaussian(frameImp.getProcessor(), 2.0);
      frameImp.setRoi(cell.toRoi(frame));
      ProfilePlot profile = new ProfilePlot(frameImp);
      Plot profilePlot = new Plot("", "", "");
      double[] yValues = profile.getProfile();
      int n = yValues.length;
      double[] xValues = new double[n];
      for (int i = 0; i < n; i++) {
        xValues[i] = i / (double) n;
      }
      profilePlot.add("line", xValues, yValues);
      picksPlots[frame - f0] = PicksFinder.findPicks(profilePlot);

      float[] floatPicksYValues = picksPlots[frame - f0].getYValues();
      if (floatPicksYValues.length > 0) {
        double max = IntStream.range(0, floatPicksYValues.length)
            .mapToDouble(i -> floatPicksYValues[i]).max().getAsDouble();
        double min = IntStream.range(0, floatPicksYValues.length)
            .mapToDouble(i -> floatPicksYValues[i]).min().getAsDouble();
        maxValue = Math.max(maxValue, (float) max);
        minValue = Math.min(minValue, (float) min);
      }
    }

    float valuesRange = maxValue - minValue;

    for (int i = 0; i < nFrames; i++) {
      Plot picksPlot = picksPlots[i];
      float[] floatPicksXValues = picksPlot.getXValues();
      float[] floatPicksYValues = picksPlot.getYValues();
      double x = i + f0;
      for (int pi = 0; pi < floatPicksXValues.length; pi++) {
        double y = floatPicksXValues[pi];
        float hue = 0.16f * (1 - ((floatPicksYValues[pi] - minValue) / valuesRange));
        plot.setColor(Color.getHSBColor(hue, 1f, 1f));
        plot.add("circle", new double[] {x}, new double[] {y});
      }
    }
    plot.show();
    plot.setLimits(f0 - 0.5, fN - 0.5, 0, 1);
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
