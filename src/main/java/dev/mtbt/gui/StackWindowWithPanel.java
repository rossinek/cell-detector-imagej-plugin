package dev.mtbt.gui;

import ij.ImagePlus;
import ij.gui.StackWindow;
import java.awt.Panel;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.awt.Component;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class StackWindowWithPanel extends StackWindow {
  Panel mainPanel;
  JPanel sidePanel;
  BooleanSupplier confirmCloseSupplier = () -> true;

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

    // IJ Fix: Emit imageOpened event manually.
    // ImagePlus does not emit it if custom window is used
    try {
      Method m = ImagePlus.class.getDeclaredMethod("notifyListeners", int.class);
      m.setAccessible(true);
      m.invoke(imp, 0 /* ImagePlus.OPENED */);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalAccessError(
          "[StackWindowWithPanel] ImagePlus implementation has been changed. "
              + "StackWindowWithPanel is no longer compatible");
    }
  }

  public void setConfirmationMethod(BooleanSupplier confirmSupplier) {
    this.confirmCloseSupplier = confirmSupplier;
  }

  public JPanel getSidePanel() {
    return this.sidePanel;
  }

  @Override
  public boolean close() {
    if (this.confirmCloseSupplier.getAsBoolean()) {
      return super.close();
    }
    return false;
  }
}
