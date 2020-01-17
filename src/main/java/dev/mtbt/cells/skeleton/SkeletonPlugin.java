package dev.mtbt.cells.skeleton;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.awt.Point;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import javax.swing.JDialog;

import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import org.scijava.widget.NumberWidget;
import dev.mtbt.HyperstackHelper;
import dev.mtbt.ShapeIndexMap;
import dev.mtbt.gui.RunnableSlider;
import dev.mtbt.util.Pair;

public abstract class SkeletonPlugin implements Command {

  @Parameter
  protected ImagePlus imp;

  @Parameter
  protected UIService uiService;

  // @Parameter(persist = false, label = "Shape index map gaussian blur radius",
  // style = NumberWidget.SCROLL_BAR_STYLE, min = "0", max = "10", stepSize = "0.1")
  // protected double blurRadiusInput;

  // @Parameter(persist = false, label = "Shape index map threshold",
  // style = NumberWidget.SCROLL_BAR_STYLE, min = "-1", max = "1", stepSize = "0.1")
  // protected double thresholdInput;

  protected ImagePlus impIndexMap = null;
  protected Skeleton skeleton = null;

  private RunnableSlider channelSlider;
  private RunnableSlider frameSlider;
  private RunnableSlider blurRadiusSlider;
  private RunnableSlider thresholdSlider;

  @Override
  public void run() {
    if (this.imp == null) {
      return;
    }

    int channel = Math.max(1, Math.min(this.channelInput, imp.getNChannels()));
    this.channelSlider = new RunnableSlider(1, imp.getNChannels(), channel, this::preview);
    this.channelSlider = new RunnableSlider(1, imp.getNFrames(), imp.getFrame(), this::preview);
    this.blurRadiusSlider = new RunnableSlider(0.0, 10.0, 4.0, this::preview);
    this.thresholdSlider = new RunnableSlider(-1.0, 1.0, 0.0, this::preview);
  }

  public void preview() {
    computeShapeIndexMap();
    thresholdShapeIndexMap();
    impIndexMap.show();
  }

  protected void close() {
    impIndexMap.close();
    Window[] windows = Window.getWindows();
    for (Window window : windows) {
      if (JDialog.class.isInstance(window)) {
        JDialog dialog = (JDialog) window;
        if (dialog.getTitle().contains(this.getInfo().getTitle())) {
          dialog.dispose();
          return;
        }
      }
    }
    uiService.showDialog("Please close plugin window manually");
  }

  private void computeShapeIndexMap() {
    this.skeleton = null;
    ImagePlus frame = HyperstackHelper.extractFrame(imp, channelInput, imp.getSlice(), frameInput);
    ImagePlus indexMap = ShapeIndexMap.getShapeIndexMap(frame, blurRadiusInput);
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
      fp.setf(i, val > thresholdInput ? val : Float.NEGATIVE_INFINITY);
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
