package dev.mtbt.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class DialogWindow extends JFrame {
  private JPanel contentPanel;
  private RunnableButton cancelButton;
  private RunnableButton okButton;

  private Runnable onCancel;
  private Runnable onOk;

  public DialogWindow(String title, Runnable onOk, Runnable onCancel) {
    super(title);
    this.onCancel = onCancel;
    this.onOk = onOk;
    initComponents();
  }

  private void initComponents() {
    JPanel buttonPanel = new JPanel();
    Box buttonBox = new Box(BoxLayout.X_AXIS);

    this.contentPanel = new JPanel();
    this.contentPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
    this.contentPanel.setLayout(new FlowLayout());

    this.cancelButton = new RunnableButton("cancel", this::cancel);
    this.okButton = new RunnableButton("ok", this::ok);

    buttonPanel.setLayout(new BorderLayout());
    buttonPanel.add(new JSeparator(), BorderLayout.NORTH);

    buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
    buttonBox.add(this.cancelButton);
    buttonBox.add(Box.createHorizontalStrut(50));
    buttonBox.add(this.okButton);
    buttonPanel.add(buttonBox, java.awt.BorderLayout.EAST);
    this.getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
    this.getContentPane().add(this.contentPanel, java.awt.BorderLayout.CENTER);
  }

  public void setContent(Component content) {
    this.contentPanel.add(content);
    this.pack();
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
