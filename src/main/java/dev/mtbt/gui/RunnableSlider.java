package dev.mtbt.gui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RunnableSlider extends JSlider implements ChangeListener {
  Runnable onChange;

  public RunnableSlider(int min, int max, int value, Runnable onChange) {
    super(JSlider.HORIZONTAL, min, max, value);
    this.onChange = onChange;
    this.addChangeListener(this);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (this.onChange != null)
      this.onChange.run();
  }
}
