package dev.mtbt.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class RunnableButton extends JButton implements ActionListener {
  Runnable callback;

  public RunnableButton(String label, Runnable cb) {
    super(label);
    this.callback = cb;
    this.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.callback.run();
  }
}
