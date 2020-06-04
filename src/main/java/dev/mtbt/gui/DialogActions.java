package dev.mtbt.gui;

import java.awt.BorderLayout;
import javax.swing.*;

public class DialogActions extends JPanel {
  private RunnableButton cancelButton;
  private RunnableButton okButton;

  private Runnable onCancel;
  private Runnable onOk;

  public DialogActions(Runnable onOk, Runnable onCancel) {
    super();
    this.onCancel = onCancel;
    this.onOk = onOk;
    initComponents();
  }

  private void initComponents() {
    this.cancelButton = new RunnableButton("cancel", this::cancel);
    this.okButton = new RunnableButton("ok", this::ok);

    this.setLayout(new BorderLayout());
    this.add(new JSeparator(), BorderLayout.NORTH);

    this.add(this.cancelButton, BorderLayout.WEST);
    this.add(Box.createHorizontalStrut(10));
    this.add(this.okButton, BorderLayout.EAST);
  }

  public void ok() {
    if (this.onOk != null)
      this.onOk.run();
  }

  public void cancel() {
    if (this.onCancel != null)
      this.onCancel.run();
  }
}
