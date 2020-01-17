package dev.mtbt.cells.skeleton;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.awt.Point;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import dev.mtbt.HyperstackHelper;
import dev.mtbt.ShapeIndexMap;
import dev.mtbt.gui.DialogWindow;
import dev.mtbt.gui.RunnableSlider;
import dev.mtbt.gui.RunnableSliderDouble;
import dev.mtbt.util.Pair;

public abstract class SkeletonPlugin implements Command {

  @Parameter
  protected ImagePlus imp;

  @Parameter
  protected UIService uiService;

  protected ImagePlus impIndexMap = null;
  protected Skeleton skeleton = null;

  protected DialogWindow dialog;
  protected JPanel dialogContent;
  protected RunnableSlider channelSlider;
  protected RunnableSlider frameSlider;
  protected RunnableSliderDouble blurRadiusSlider;
  protected RunnableSliderDouble thresholdSlider;

  @Override
  public void run() {
    if (this.imp == null) {
      return;
    }

    this.dialogContent = new JPanel();
    dialogContent.setLayout(new BoxLayout(dialogContent, BoxLayout.Y_AXIS));

    int channel = Math.min(1, imp.getNChannels());
    this.channelSlider = new RunnableSlider(1, imp.getNChannels(), channel, this::preview);
    channelSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
    dialogContent.add(channelSlider);

    this.frameSlider = new RunnableSlider(1, imp.getNFrames(), imp.getFrame(), this::preview);
    frameSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
    dialogContent.add(frameSlider);

    this.blurRadiusSlider = new RunnableSliderDouble(0.0, 10.0, 0.2, 4.0, this::preview);
    blurRadiusSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
    dialogContent.add(blurRadiusSlider);

    this.thresholdSlider = new RunnableSliderDouble(-1.0, 1.0, 0.1, 0.0, this::preview);
    thresholdSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
    dialogContent.add(thresholdSlider);

    this.dialog = new DialogWindow("Skeleton plugin", this::done, this::cleanup);
    this.dialog.setVisible(true);
  }

  public void preview() {
    computeShapeIndexMap();
    thresholdShapeIndexMap();
    impIndexMap.show();
  }

  protected void done() {
    this.dialog.setVisible(false);
  }

  protected void cleanup() {
    impIndexMap.close();
    this.dialog.setVisible(false);
  }

  private void computeShapeIndexMap() {
    this.skeleton = null;
    ImagePlus frame = HyperstackHelper.extractFrame(imp, channelSlider.getValue(), imp.getSlice(),
        frameSlider.getValue());
    ImagePlus indexMap = ShapeIndexMap.getShapeIndexMap(frame, blurRadiusSlider.getDoubleValue());
    if (impIndexMap == null) {
      impIndexMap = indexMap;
    } else {
      impIndexMap.setProcessor(indexMap.getProcessor());
    }
  }

  private void thresholdShapeIndexMap() {
    FloatProcessor fp = (FloatProcessor) impIndexMap.getProcessor();
    int length = impIndexMap.getProcessor().getPixelCount();
    for (int i = 0; i < length; i++) {
      float val = fp.getf(i);
      fp.setf(i, val > thresholdSlider.getDoubleValue() ? val : Float.NEGATIVE_INFINITY);
    }
  }

  protected Spine performSearch(Point point) {
    return this.performSearch(point, null);
  }

  protected Spine performSearch(Point point, Spine previous) {
    if (this.skeleton == null) {
      this.skeleton = new Skeleton(this.impIndexMap);
    }
    Spine spine = this.skeleton.findSpine(point, previous);
    return spine;
  }

  protected List<Spine> performSearch(List<Point> points, Spine previous) {
    if (points.isEmpty()) {
      return new ArrayList<Spine>();
    }

    ArrayList<Pair<Point, Spine>> spines =
        points.stream().map(point -> new Pair<>(point, this.performSearch(point, previous)))
            .collect(Collectors.toCollection(ArrayList::new));

    return this.fixConflicts(spines).stream().map(pair -> pair.getValue())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  protected List<Pair<Point, Spine>> fixConflicts(List<Pair<Point, Spine>> spines) {
    boolean change;
    do {
      change = false;
      ListIterator<Pair<Point, Spine>> iterator = spines.listIterator();
      while (iterator.hasNext()) {
        Pair<Point, Spine> spine = iterator.next();
        for (int i = iterator.nextIndex(); i < spines.size(); i++) {
          if (spine.getValue().overlaps(spines.get(i).getValue())) {
            Point p1 = spine.getKey();
            Point p2 = spines.get(i).getKey();
            Spine[] newSpines = spine.getValue().split(new dev.mtbt.graph.Point(p1),
                new dev.mtbt.graph.Point(p2), p -> this.impIndexMap.getProcessor().getf(p.x, p.y));
            spine.getValue().assign(newSpines[0]);
            spines.get(i).getValue().assign(newSpines[1]);
            change = true;
            break;
          }
        }
      }
    } while (change);

    return spines;
  }
}
