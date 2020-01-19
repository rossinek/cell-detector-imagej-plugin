package dev.mtbt.gui;

import java.awt.Dimension;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RunnableSlider extends JSlider implements ChangeListener {
  Runnable onChange;

  public RunnableSlider(int min, int max, int value, Runnable onChange) {
    super(JSlider.HORIZONTAL, min, max, value);
    this.onChange = onChange;
    this.addChangeListener(this);

    this.setPaintTicks(true);
    this.setMinorTickSpacing(1);
    Dimension size = this.getPreferredSize();
    this.setPreferredSize(new Dimension(size.width + 200, size.height));
  }

  public void showLables(Number step) {
    this.setLabelTable(this.createStandardLabels(step.intValue()));
    this.setPaintLabels(true);
    this.setMajorTickSpacing(step.intValue());
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (this.onChange != null)
      this.onChange.run();
  }
}
