package dev.mtbt.gui;

import java.awt.Component;
import java.awt.BorderLayout;
import javax.swing.*;

public class ExpandablePanel extends JPanel {
  RunnableButton toggleButton;
  Component content;
  Runnable onChange;

  public ExpandablePanel(String buttonLabel, Component content) {
    this(buttonLabel, content, null);
  }

  public ExpandablePanel(String buttonLabel, Component content, Runnable onChange) {
    super();
    this.onChange = onChange;
    this.setLayout(new BorderLayout());
    this.toggleButton = new RunnableButton(buttonLabel, this::togglePanel);
    this.content = content;

    this.add(toggleButton, BorderLayout.NORTH);
    this.toggleButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

    this.add(content, BorderLayout.CENTER);
    this.content.setVisible(false);
  }

  public void togglePanel() {
    this.content.setVisible(!this.content.isVisible());
    if (this.onChange != null) {
      this.onChange.run();
    }
  }
}
