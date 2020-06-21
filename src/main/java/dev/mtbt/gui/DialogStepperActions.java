package dev.mtbt.gui;

import java.awt.BorderLayout;
import javax.swing.*;

public class DialogStepperActions extends JPanel {
  private RunnableButton cancelButton;
  private RunnableButton nextButton;
  private RunnableButton previousButton;

  private Runnable onCancel;
  private Runnable onPrevious;
  private Runnable onNext;

  public DialogStepperActions(Runnable onPrevious, Runnable onNext, Runnable onCancel) {
    super();
    this.onPrevious = onPrevious;
    this.onNext = onNext;
    this.onCancel = onCancel;
    initComponents();
    this.setIsFirst(true);
  }

  private void initComponents() {
    this.cancelButton = new RunnableButton("Cancel", this::cancel);
    this.previousButton = new RunnableButton("<< Back", this::previous);
    this.nextButton = new RunnableButton("Next >>", this::next);
    JPanel navPanel = new JPanel();
    navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.X_AXIS));
    navPanel.add(this.previousButton);
    navPanel.add(this.nextButton);

    this.setLayout(new BorderLayout());
    this.add(new JSeparator(), BorderLayout.NORTH);

    this.add(this.cancelButton, BorderLayout.WEST);
    this.add(Box.createHorizontalStrut(10));
    this.add(navPanel, BorderLayout.EAST);
  }

  public void setIsFirst(boolean isFirst) {
    this.previousButton.setEnabled(!isFirst);
  }

  public void setIsLast(boolean isLast) {
    this.nextButton.setText(isLast ? "Finish >>" : "Next >>");
  }

  public void next() {
    if (this.onNext != null)
      this.onNext.run();
  }

  public void previous() {
    if (this.onPrevious != null)
      this.onPrevious.run();
  }

  public void cancel() {
    if (this.onCancel != null)
      this.onCancel.run();
  }
}
