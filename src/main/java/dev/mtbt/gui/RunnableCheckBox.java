package dev.mtbt.gui;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.*;

public class RunnableCheckBox extends JCheckBox implements ItemListener {
  Runnable callback;

  public RunnableCheckBox(String label, Runnable cb) {
    super(label);
    this.callback = cb;
    this.addItemListener(this);
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (this.callback != null) {
      this.callback.run();
    }
  }
}
