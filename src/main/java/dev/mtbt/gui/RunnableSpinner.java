package dev.mtbt.gui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RunnableSpinner extends JSpinner implements ChangeListener {
  Runnable onChange;

  public RunnableSpinner(Number value, Comparable min, Comparable max, Runnable onChange) {
    this(value, min, max, 1, onChange);
  }

  public RunnableSpinner(Number value, Comparable min, Comparable max, Number step,
      Runnable onChange) {
    super(new SpinnerNumberModel(value, min, max, step));
    this.onChange = onChange;
    this.addChangeListener(this);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (this.onChange != null)
      this.onChange.run();
  }
}
