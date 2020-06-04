package dev.mtbt.gui;

import ij.ImagePlus;
import ij.gui.StackWindow;
import java.awt.Panel;
import java.awt.Component;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class StackWindowWithPanel extends StackWindow {
  Panel mainPanel;
  JPanel sidePanel;

  public StackWindowWithPanel(ImagePlus imp) {
    super(imp);
    this.setLayout(new BorderLayout());
    Component[] comps = this.getComponents();
    for (Component comp : comps) {
      this.remove(comp);
    }

    this.mainPanel = new Panel();
    this.mainPanel.setLayout(new BoxLayout(this.mainPanel, BoxLayout.Y_AXIS));

    Panel panel = new Panel(new BorderLayout());
    this.sidePanel = new JPanel();
    this.sidePanel.setLayout(new BorderLayout());
    panel.add(this.sidePanel);

    for (Component comp : comps) {
      this.mainPanel.add(comp);
    }

    this.add(panel, BorderLayout.WEST);
    this.add(this.mainPanel, BorderLayout.CENTER);
    this.pack();
  }

  public JPanel getSidePanel() {
    return this.sidePanel;
  }
}
