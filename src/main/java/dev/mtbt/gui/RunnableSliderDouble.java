package dev.mtbt.gui;

import java.awt.Component;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.swing.JLabel;

public class RunnableSliderDouble extends RunnableSlider {
  double scale;

  public RunnableSliderDouble(double min, double max, double step, double val, Runnable onChange) {
    super((int) (min / step), (int) (max / step), (int) (val / step), onChange);
    scale = step;
  }

  @Override
  public void showLables(Number step) {
    int normalizedStep = (int) (step.doubleValue() / scale);
    this.setPaintLabels(true);
    this.setMajorTickSpacing(normalizedStep);

    Dictionary<Integer, Component> labelTable = new Hashtable<Integer, Component>();
    for (int value = getMinimum(); value <= getMaximum(); value += normalizedStep) {
      labelTable.put(value, new JLabel("" + (value * scale)));
    }
    this.setLabelTable(labelTable);
  }


  public double getDoubleValue() {
    return this.getValue() * scale;
  }
}
