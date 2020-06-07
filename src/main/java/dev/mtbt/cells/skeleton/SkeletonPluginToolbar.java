package dev.mtbt.cells.skeleton;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import dev.mtbt.cells.CellObserver;
import dev.mtbt.gui.ClearableButtonGroup;
import ij.IJ;
import ij.IJEventListener;

public class SkeletonPluginToolbar extends JPanel implements ActionListener, IJEventListener {
  Consumer<String> listener;
  ClearableButtonGroup buttonGroup;
  List<JToggleButton> buttons = new ArrayList<>();
  Color backgroundDefault = Color.WHITE;
  Color backgroundActive = Color.LIGHT_GRAY;
  Border borderDefault =
      BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2),
          BorderFactory.createEmptyBorder(5, 5, 5, 5));
  Border borderActive = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(Color.RED, 2), BorderFactory.createEmptyBorder(5, 5, 5, 5));


  public SkeletonPluginToolbar() {
    this((cmd) -> {
      // ignore
    });
  }

  public SkeletonPluginToolbar(Consumer<String> listener) {
    super();
    this.listener = listener;
    this.buttonGroup = new ClearableButtonGroup();

    this.addButton(CellObserver.TOOL_CUT, "/images/cut.png");
    this.addButton(CellObserver.TOOL_ERASE, "/images/erase.png");

    IJ.addEventListener(this);
  }

  private void addButton(String action, String iconPath) {
    JToggleButton button = new JToggleButton(new ImageIcon(getClass().getResource(iconPath)));
    button.setActionCommand(action);
    button.addActionListener(this);
    button.setOpaque(true);
    button.setBorder(this.borderDefault);
    button.setBackground(this.backgroundDefault);
    button.setContentAreaFilled(true);
    button.addItemListener((e) -> {
      JToggleButton b = (JToggleButton) e.getItem();
      b.setBorder(b.isSelected() ? borderActive : borderDefault);
      b.setBackground(b.isSelected() ? backgroundActive : backgroundDefault);
    });

    buttonGroup.add(button);
    this.buttons.add(button);
    this.add(button);
  }

  public JToggleButton getActiveButton() {
    Optional<JToggleButton> optionalButton =
        this.buttons.stream().filter(b -> b.isSelected()).findFirst();
    return optionalButton.isPresent() ? optionalButton.get() : null;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    boolean isSelected = this.buttonGroup.getSelection() != null;
    String tool = isSelected ? e.getActionCommand() : null;
    CellObserver.setActiveTool(tool);
    listener.accept(tool);
  }

  @Override
  public void eventOccurred(int eventID) {
    JToggleButton activeButton = getActiveButton();
    if (eventID == IJEventListener.TOOL_CHANGED && activeButton != null
        && IJ.getToolName() != activeButton.getActionCommand()) {
      this.buttonGroup.clearSelection();
      CellObserver.setActiveTool(null);
    }
  }
}
