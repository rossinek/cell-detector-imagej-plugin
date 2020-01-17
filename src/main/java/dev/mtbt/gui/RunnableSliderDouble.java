package dev.mtbt.gui;

import javax.swing.event.ChangeListener;

public class RunnableSliderDouble extends RunnableSlider implements ChangeListener {
  double scale;

  public RunnableSliderDouble(double min, double max, double step, double val, Runnable onChange) {
    super((int) (min / step), (int) (max / step), (int) (val / step), onChange);
    scale = step;
  }

  public double getDoubleValue() {
    return this.getValue() * scale;
  }
}
