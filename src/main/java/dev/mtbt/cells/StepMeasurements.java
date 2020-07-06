package dev.mtbt.cells;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import dev.mtbt.HyperstackHelper;
import dev.mtbt.ImageJUtils;
import dev.mtbt.gui.RunnableButton;
import dev.mtbt.gui.RunnableSpinner;
import dev.mtbt.util.PicksFinder;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.ProfilePlot;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.GaussianBlur;

public class StepMeasurements implements ICellsPluginStep {

  private ImagePlus imp;
  private CellCollection cellCollection;
  private JComboBox<String> measurementsCellNameSelect;

  private RunnableSpinner fluorescentChannelSpinner;
  private RunnableSpinner profileDataLengthSpinner;

  public StepMeasurements() {
  }

  @Override
  public JPanel init(ImagePlus imp, CellCollection cellCollection) {
    this.imp = imp;
    this.cellCollection = cellCollection;

    JPanel dialogContent = new JPanel();
    dialogContent.setLayout(new BoxLayout(dialogContent, BoxLayout.Y_AXIS));

    this.fluorescentChannelSpinner = new RunnableSpinner(1, 1, imp.getNChannels(), () -> {
    });
    this.addCenteredComponent(dialogContent, new JLabel("Fluorescent channel"));
    this.addCenteredComponent(dialogContent, this.fluorescentChannelSpinner);

    dialogContent.add(Box.createVerticalStrut(15));

    this.measurementsCellNameSelect = new JComboBox<>();
    this.addCenteredComponent(dialogContent, new JLabel("Target cell"));
    this.addCenteredComponent(dialogContent, this.measurementsCellNameSelect);
    this.updateCellsModel();

    dialogContent.add(Box.createVerticalStrut(15));

    RunnableButton plotLengthButton = new RunnableButton("Plot length", this::plotLengths);
    this.addCenteredComponent(dialogContent, plotLengthButton);

    RunnableButton plotProfileButton =
        new RunnableButton("Plot profile in time", this::plotProfile);
    this.addCenteredComponent(dialogContent, plotProfileButton);

    RunnableButton plotAverageProfileButton =
        new RunnableButton("Plot average profile", this::plotAverageProfile);
    this.addCenteredComponent(dialogContent, plotAverageProfileButton);

    RunnableButton plotProfilePicksButton =
        new RunnableButton("Show profile picks", this::plotProfilePicks);
    this.addCenteredComponent(dialogContent, plotProfilePicksButton);

    dialogContent.add(Box.createVerticalStrut(15));
    dialogContent.add(new JSeparator());
    dialogContent.add(Box.createVerticalStrut(15));

    this.profileDataLengthSpinner = new RunnableSpinner(200, 10, 1000, () -> {
    });
    this.addCenteredComponent(dialogContent, new JLabel("Profile length (#samples)"));
    this.addCenteredComponent(dialogContent, this.profileDataLengthSpinner);

    RunnableButton exportCellDataButton =
        new RunnableButton("Export cell data", this::exportCellData);
    this.addCenteredComponent(dialogContent, exportCellDataButton);

    RunnableButton exportAllDataButton =
        new RunnableButton("Export all cells data", this::exportAllCellsData);
    this.addCenteredComponent(dialogContent, exportAllDataButton);

    return dialogContent;
  }

  private void addCenteredComponent(JPanel panel, JComponent component) {
    component.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(component);
  }

  private void updateCellsModel() {
    if (this.measurementsCellNameSelect != null) {
      List<String> cells = CellAnalyzer.getAllGenerationsNames(this.cellCollection);
      this.measurementsCellNameSelect
          .setModel(new DefaultComboBoxModel<>(cells.toArray(new String[cells.size()])));
    }
  }

  protected void showLengthsTable() {
    Cell cell = this.getCurrentCell();
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
    Cell cell = this.getCurrentCell();
    ResultsTable table = this.measureLengths(cell);

    table.getColumnAsDoubles(0);
    double[] frames =
        IntStream.range(cell.getF0(), cell.getFN()).mapToDouble(i -> (double) i).toArray();
    Plot plot = new Plot("Length plot", "Frame index", "Length");
    plot.add("line", frames, table.getColumnAsDoubles(0));
    plot.show();
  }

  private ProfilePlot getFrameProfile(Cell cell, int frame) {
    return this.getFrameProfile(cell, frame, 0);
  }

  private ProfilePlot getFrameProfile(Cell cell, int frame, double blur) {
    ImagePlus frameImp = HyperstackHelper.extractFrame(imp,
        (int) this.fluorescentChannelSpinner.getValue(), imp.getSlice(), frame);
    if (blur > 0) {
      new GaussianBlur().blurGaussian(frameImp.getProcessor(), blur);
    }
    frameImp.setRoi(cell.toRoi(frame));
    return new ProfilePlot(frameImp);
  }

  private Cell getCurrentCell() {
    String cellName = (String) this.measurementsCellNameSelect.getSelectedItem();
    return CellAnalyzer.getCellByName(this.cellCollection, cellName);
  }

  protected Plot plotProfile() {
    Cell cell = this.getCurrentCell();
    int f0 = cell.getF0();
    int fN = cell.getFN();
    Plot plot = this.getFrameProfile(cell, f0).getPlot();
    plot.addToStack();
    for (int frame = f0 + 1; frame < fN; frame++) {
      plot.getStack().addPlot(this.getFrameProfile(cell, frame).getPlot());
    }
    plot.show();
    WindowManager.getCurrentImage().setTitle("Profile of " + cell.getName());
    return plot;
  }

  protected void plotAverageProfile() {
    Cell cell = this.getCurrentCell();
    int nFrames = cell.getFN() - cell.getF0();
    double[][] profiles = new double[nFrames][];
    int maxLength = 0;
    for (int index = 0; index < nFrames; index++) {
      profiles[index] = this.getFrameProfile(cell, index + cell.getF0()).getProfile();
      maxLength = Math.max(maxLength, profiles[index].length);
    }

    Plot avgProfilePlot =
        new Plot("Average profile of " + cell.getName(), "Position on the body (0-1)", "Intensity");
    double[] xValues = new double[maxLength];
    double[] yValues = new double[maxLength];
    for (int index = 0; index < maxLength; index++) {
      double value = 0;
      for (double[] profile : profiles) {
        double profileIndex = ((double) index / maxLength) * profile.length;
        value += sampleData(profile, profileIndex);
      }
      xValues[index] = (double) index / maxLength;
      yValues[index] = value / nFrames;
    }

    avgProfilePlot.add("line", xValues, yValues);
    avgProfilePlot.show();
  }

  protected void plotProfilePicks() {
    Cell cell = this.getCurrentCell();
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
      ProfilePlot profile = getFrameProfile(cell, frame, 2.0);
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

  protected void exportCellData() {
    Cell cell = this.getCurrentCell();
    SaveDialog sd = new SaveDialog("Save Cell Data", "Cell " + cell.getName(), ".csv");
    String file = sd.getFileName();
    if (file != null) {
      try {
        this.getCellData(cell, (int) this.profileDataLengthSpinner.getValue(), false)
            .saveAs(sd.getDirectory() + file);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  protected ResultsTable getCellData(Cell cell, int profileLength, boolean addName) {
    ResultsTable table = new ResultsTable();
    int f0 = cell.getF0();
    int fN = cell.getFN();
    for (int frame = f0; frame < fN; frame++) {
      table.incrementCounter();
      if (addName) {
        table.addValue("name", cell.getName());
      }
      table.addValue("frame", frame);
      table.addValue("length", cell.getFrame(frame).getLength());
      double[] profile = getFrameProfile(cell, frame).getProfile();
      for (int i = 0; i < profileLength; i++) {
        double profileIndex = ((double) i / profileLength) * profile.length;
        table.addValue("p" + i, this.sampleData(profile, profileIndex));
      }
    }
    return table;
  }


  private double sampleData(double[] data, double dataIndex) {
    int floorIndex = (int) Math.max(0, Math.floor(dataIndex));
    int ceilIndex = (int) Math.min(Math.ceil(dataIndex), data.length - 1);
    double ceilWeight = dataIndex - floorIndex;
    double floorWeight = 1.0 - ceilWeight;
    return floorWeight * data[floorIndex] + ceilWeight * data[ceilIndex];
  }

  protected void exportAllCellsData() {
    SaveDialog sd = new SaveDialog("Save Cells Data", "Cells Data", ".csv");
    String file = sd.getFileName();
    ResultsTable table = new ResultsTable();
    if (file != null) {
      try {
        List<Cell> cells = CellAnalyzer.getAllGenerations(this.cellCollection);
        for (Cell cell : cells) {
          ResultsTable cellTable =
              this.getCellData(cell, (int) this.profileDataLengthSpinner.getValue(), true);
          ImageJUtils.appendResultsTable(table, cellTable);
        }
        table.show("Data table");
        table.saveAs(sd.getDirectory() + file);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void imageUpdated() {
    this.updateCellsModel();
  }

  @Override
  public void cleanup() {
  }
}
