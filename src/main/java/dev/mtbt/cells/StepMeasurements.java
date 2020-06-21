package dev.mtbt.cells;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import dev.mtbt.HyperstackHelper;
import dev.mtbt.gui.RunnableButton;
import dev.mtbt.util.PicksFinder;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.ProfilePlot;
import ij.measure.ResultsTable;
import ij.plugin.filter.GaussianBlur;

public class StepMeasurements implements ICellsPluginStep {

  private ImagePlus imp;
  private CellCollection cellCollection;
  private JComboBox<String> measurementsCellNameSelect;

  public StepMeasurements() {
  }

  @Override
  public JPanel init(ImagePlus imp, CellCollection cellCollection) {
    this.imp = imp;
    this.cellCollection = cellCollection;

    JPanel dialogContent = new JPanel();
    dialogContent.setLayout(new BoxLayout(dialogContent, BoxLayout.Y_AXIS));
    measurementsCellNameSelect = new JComboBox<>();
    measurementsCellNameSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
    dialogContent.add(measurementsCellNameSelect);

    List<String> cells = CellAnalyzer.getAllGenerationsNames(cellCollection);
    this.measurementsCellNameSelect
        .setModel(new DefaultComboBoxModel<>(cells.toArray(new String[cells.size()])));

    RunnableButton measureLengthsButton =
        new RunnableButton("Measure lengths", this::showLengthsTable);
    measureLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    dialogContent.add(measureLengthsButton);

    RunnableButton plotLengthsButton = new RunnableButton("Plot lengths", this::plotLengths);
    plotLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    dialogContent.add(plotLengthsButton);

    RunnableButton plotProfileButton = new RunnableButton("Plot profile", this::plotProfile);
    plotLengthsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    dialogContent.add(plotProfileButton);

    return dialogContent;
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

  @Override
  public void imageUpdated() {
  }

  @Override
  public void cleanup() {
  }
}
