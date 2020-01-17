package dev.mtbt.gui;

import java.awt.Component;

public class DialogStepperStep {
  private DialogStepper dialog;
  private String name;
  private Component component;
  private boolean finished = false;

  public DialogStepperStep(DialogStepper dialog, String name, Component component) {
    this.dialog = dialog;
    this.component = component;
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public Component getComponent() {
    return this.component;
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
    dialog.update();
  }

  public boolean isFinished() {
    return this.finished;
  }
}
