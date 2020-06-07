/*
 * Based on by http://imagej.net/Find_Peaks by Tiago Ferreira, v1.0.5 2016.03
 */
package dev.mtbt.util;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.plugin.filter.MaximumFinder;
import ij.util.ArrayUtil;
import ij.util.Tools;
import java.awt.Color;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Development>Utils>Find local maxima")
public class PicksFinder extends DynamicCommand {
  double tolerance = 0d;
  double minPeakDistance = 0d;
  double minMaximaValue = Double.NaN;
  boolean excludeOnEdges = false;
  boolean listValues = false;

  double[] xvalues;
  double[] yvalues;

  int[] findMaxima(double[] values, double tolerance) {
    return MaximumFinder.findMaxima(values, tolerance, excludeOnEdges);
  }

  double[] getCoordinates(double[] values, int[] positions) {
    int size = positions.length;
    double[] cc = new double[size];
    for (int i = 0; i < size; i++)
      cc[i] = values[positions[i]];
    return cc;
  }

  boolean prompt() {
    GenericDialog gd = new GenericDialog("Find Local Maxima...");
    gd.addNumericField("Min._peak_amplitude:", tolerance, 2);
    gd.addNumericField("Min._peak_distance:", minPeakDistance, 2);
    gd.addNumericField("Min._value of maxima:", minMaximaValue, 2, 6, "(NaN: no filtering)");
    gd.addCheckbox("Exclude peaks on edges of plot", excludeOnEdges);
    gd.addCheckbox("List values", listValues);
    gd.addHelp("http://imagej.net/Find_Peaks");
    gd.showDialog();
    tolerance = gd.getNextNumber();
    minPeakDistance = gd.getNextNumber();
    minMaximaValue = gd.getNextNumber();
    excludeOnEdges = gd.getNextBoolean();
    listValues = gd.getNextBoolean();
    return !gd.wasCanceled();
  }

  int[] trimPeakHeight(int[] positions) {
    int size1 = positions.length;
    int size2 = 0;
    for (int i = 0; i < size1; i++) {
      if (filteredHeight(yvalues[positions[i]]))
        size2++;
      else
        break; // positions are sorted by amplitude
    }
    int[] newpositions = new int[size2];
    for (int i = 0; i < size2; i++)
      newpositions[i] = positions[i];
    return newpositions;
  }

  boolean filteredHeight(double height) {
    return (height > minMaximaValue);
  }

  int[] trimPeakDistance(int[] positions) {
    int size = positions.length;
    int[] temp = new int[size];
    int newsize = 0;
    for (int i = size - 1; i >= 0; i--) {
      int pos1 = positions[i];
      boolean trim = false;
      for (int j = i - 1; j >= 0; j--) {
        int pos2 = positions[j];
        if (Math.abs(xvalues[pos2] - xvalues[pos1]) < minPeakDistance) {
          trim = true;
          break;
        }
      }
      if (!trim)
        temp[newsize++] = pos1;
    }
    int[] newpositions = new int[newsize];
    for (int i = 0; i < newsize; i++)
      newpositions[i] = temp[i];
    return newpositions;
  }

  protected Plot findPicksInstance(Plot plot, double tolerance, double minMaximaValue,
      double minPeakDistance) {
    this.tolerance = tolerance;
    this.yvalues = Tools.toDouble(plot.getYValues());
    this.xvalues = Tools.toDouble(plot.getXValues());
    this.minMaximaValue = minMaximaValue;
    this.minPeakDistance = minPeakDistance;

    int[] maxima = findMaxima(this.yvalues, this.tolerance);
    if (!Double.isNaN(this.minMaximaValue))
      maxima = trimPeakHeight(maxima);
    if (this.minPeakDistance > 0) {
      maxima = trimPeakDistance(maxima);
    }
    double[] xMaxima = getCoordinates(this.xvalues, maxima);
    double[] yMaxima = getCoordinates(this.yvalues, maxima);

    Plot output = new Plot("Peaks in " + plot.getTitle(), "", "");
    output.setColor(Color.RED);
    output.add("circle", xMaxima, yMaxima);
    output.addLabel(0.00, 0, maxima.length + " maxima");
    output.setColor(Color.BLACK);
    output.addLabel(0.50, 0,
        "Min. amp.: " + IJ.d2s(tolerance, 2) + "  Min. dx.: " + IJ.d2s(minPeakDistance, 2));
    return output;
  }

  public static Plot findPicks(Plot plot) {
    ArrayUtil stats = new ArrayUtil(plot.getYValues());
    double tolerance = Math.sqrt(stats.getVariance());
    return findPicks(plot, tolerance, Double.NaN, Double.NaN);
  }

  public static Plot findPicks(Plot plot, double tolerance, double minMaximaValue,
      double minPeakDistance) {
    return new PicksFinder().findPicksInstance(plot, tolerance, minMaximaValue, minPeakDistance);
  }

  @Override
  public void run() {
    PlotWindow pw;
    ImagePlus imp = WindowManager.getCurrentImage();
    if (imp == null) {
      IJ.error("There are no plots open.");
      return;
    }
    ImageWindow win = imp.getWindow();
    if (win != null && (win instanceof PlotWindow)) {
      pw = (PlotWindow) win;
      float[] fyvalues = pw.getYValues();
      ArrayUtil stats = new ArrayUtil(fyvalues);
      tolerance = Math.sqrt(stats.getVariance());
      yvalues = Tools.toDouble(fyvalues);
      xvalues = Tools.toDouble(pw.getXValues());
    } else {
      IJ.error(imp.getTitle() + " is not a plot window.");
      return;
    }

    if (!prompt())
      return;
    int[] maxima = findMaxima(yvalues, tolerance);
    if (!Double.isNaN(minMaximaValue))
      maxima = trimPeakHeight(maxima);
    if (minPeakDistance > 0) {
      maxima = trimPeakDistance(maxima);
    }
    double[] xMaxima = getCoordinates(xvalues, maxima);
    double[] yMaxima = getCoordinates(yvalues, maxima);

    String plotTitle = imp.getTitle();
    Plot plot = new Plot("Peaks in " + plotTitle, "", "");
    plot.add("line", xvalues, yvalues);
    plot.setLineWidth(2);
    plot.setColor(Color.RED);
    plot.addPoints(xMaxima, yMaxima, Plot.CIRCLE);
    plot.addLabel(0.00, 0, maxima.length + " maxima");
    plot.setColor(Color.BLACK);
    plot.addLabel(0.50, 0,
        "Min. amp.: " + IJ.d2s(tolerance, 2) + "  Min. dx.: " + IJ.d2s(minPeakDistance, 2));
    plot.setLineWidth(1);

    if (plotTitle.startsWith("Peaks in"))
      pw.drawPlot(plot);
    else
      pw = plot.show();
    if (listValues)
      pw.getResultsTable().show("Plot Values");
  }
}
