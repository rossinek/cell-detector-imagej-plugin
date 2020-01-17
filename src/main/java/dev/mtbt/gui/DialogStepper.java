package dev.mtbt.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class DialogStepper extends JFrame implements ActionListener {
  private JPanel cardPanel;

  private JButton backButton;
  private JButton nextButton;
  private JButton cancelButton;

  private int stepIndex = 0;
  private List<DialogStepperStep> steps = new ArrayList<>();

  private Runnable onCancel;

  public DialogStepper(String title, Runnable onCancel) {
    super(title);
    this.onCancel = onCancel;
    initComponents();
    pack();
  }

  private void initComponents() {
    JPanel buttonPanel = new JPanel();
    Box buttonBox = new Box(BoxLayout.X_AXIS);

    this.cardPanel = new JPanel();
    this.cardPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
    this.cardPanel.setLayout(new CardLayout());

    this.backButton = new JButton("<< Back");
    this.nextButton = new JButton("Next >>");
    this.cancelButton = new JButton("Cancel");

    this.backButton.addActionListener(this);
    this.nextButton.addActionListener(this);
    this.cancelButton.addActionListener(this);

    this.nextButton.setEnabled(false);

    buttonPanel.setLayout(new BorderLayout());
    buttonPanel.add(new JSeparator(), BorderLayout.NORTH);

    buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
    buttonBox.add(this.backButton);
    buttonBox.add(Box.createHorizontalStrut(10));
    buttonBox.add(this.nextButton);
    buttonBox.add(Box.createHorizontalStrut(30));
    buttonBox.add(this.cancelButton);
    buttonPanel.add(buttonBox, java.awt.BorderLayout.EAST);
    this.getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
    this.getContentPane().add(cardPanel, java.awt.BorderLayout.CENTER);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == this.nextButton) {
      next();
    } else if (source == this.backButton) {
      previous();
    } else if (source == this.cancelButton) {
      cancel();
    }
  }

  public void registerStep(DialogStepperStep step) {
    this.steps.add(step);
    this.cardPanel.add(step.getName(), step.getComponent());
    this.update();
  }

  public DialogStepperStep getCurrentStep() {
    return this.steps.get(this.stepIndex);
  }

  public void previous() {
    if (this.isPreviousEnabled()) {
      stepIndex--;
      update();
    }
  }

  public void next() {
    if (this.isNextEnabled()) {
      if (stepIndex < this.steps.size() - 1) {
        stepIndex++;
      } else {
        cancel();
      }
      update();
    }
  }

  public boolean isPreviousEnabled() {
    return stepIndex > 0;
  }

  public boolean isNextEnabled() {
    return stepIndex < this.steps.size() && this.getCurrentStep().isFinished();
  }

  public void update() {
    CardLayout cardLayout = (CardLayout) (this.cardPanel.getLayout());
    DialogStepperStep step = this.getCurrentStep();
    cardLayout.show(this.cardPanel, step.getName());
    this.backButton.setEnabled(this.isPreviousEnabled());
    this.nextButton.setEnabled(this.isNextEnabled());
    if (stepIndex == this.steps.size() - 1) {
      this.nextButton.setText("Finish >>");
    } else {
      this.nextButton.setText("Next >>");
    }
    this.pack();
  }

  public void cancel() {
    this.setVisible(false);
    if (this.onCancel != null)
      this.onCancel.run();
  }
}
