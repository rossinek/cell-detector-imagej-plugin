package sc.fiji;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.PointRoi;
import ij.plugin.MontageMaker;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import net.imglib2.util.Pair;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = "Developement>Stabilize stack")
public class StackStabilizer implements Command {

  @Parameter
  private ImagePlus imp;

  static float alpha = HarrisCornerDetector.DEFAULT_ALPHA;
  static int threshold = HarrisCornerDetector.DEFAULT_THRESHOLD;
  static int nmax = 0;  //points to show
  static boolean doCleanUp = true;

  @Override
  public void run () {
    ImageStack is = imp.getStack();
    System.out.println("c: " + imp.getNChannels() + ", s:" + imp.getNSlices() + ", f:" + imp.getNFrames());
    List<List<Corner>> corners = new ArrayList<>(imp.getNFrames());
    if (!showDialog())
      return; // dialog canceled or error
    for (int frame = 1; frame <= imp.getNFrames(); frame++) {
      ImageProcessor ip = is.getProcessor(imp.getStackIndex(imp.getC(), imp.getSlice(), frame));
      HarrisCornerDetector hcd = new HarrisCornerDetector(ip, alpha, threshold);
      hcd.setDoCleanup(doCleanUp);
      corners.add(hcd.findCorners());
    }

    ImageStack montageStack = new ImageStack(imp.getWidth() * 2, imp.getHeight());

    List<List<Correspondence>> correspondences = new ArrayList<>(imp.getNFrames() - 1);
    for (int frame = 2; frame <= imp.getNFrames(); frame++) {
      List<Correspondence> frameCorrespondences = new Vector<>(100);
      for (Corner c : corners.get(frame - 2)) {
        frameCorrespondences.add(findCorrespondence(c, corners.get(frame - 1)));
      }
      Collections.sort(frameCorrespondences);
      correspondences.add(frameCorrespondences);
      if (frameCorrespondences.size() == 0) continue;
      // draw it!
      // ImagePlus montage = drawCorrespondences(frameCorrespondences);
      // if (montage != null) montageStack.addSlice(montage.getProcessor());
      Map<Point, List<Correspondence>> groupedByShift = frameCorrespondences.stream().collect(Collectors.groupingBy(c -> c.shift));
      List<List<Correspondence>> lists = new ArrayList(groupedByShift.values());
      Comparator<List<Correspondence>> comparator = (l, r) -> r.size() - l.size();
      Collections.sort(lists, comparator);

      ImagePlus montage = drawCorrespondences(lists.get(0));
      System.out.println("==================");
      System.out.println("frame: " + (frame - 1) + " size: " + lists.get(0).size());
      for(int i = 1; i < lists.size() && lists.get(i).size() > 1; i++) {
        drawCorrespondences(lists.get(i), montage);
        System.out.println("frame: " + (frame - 1) + " size: " + lists.get(i).size());
      }
      montageStack.addSlice(montage.getProcessor());
    }
    ImagePlus montages = new ImagePlus("Montages", montageStack);
    montages.show();

    //    for ()
    //    int index = imp.getStackIndex(imp.getChannel(), slice, frame);
    //    ImageProcessor ip = stack.getProcessor(index);
    //        if (!showDialog()) return; //dialog canceled or error
    //        HarrisCornerDetector hcd = new HarrisCornerDetector(imp.getProcessor(), alpha, threshold);
    //    imp.getStack();
    //    ImagePlus ii = new ImagePlus();
    //    ii.setStack(imp.getStack());
    //    ii.show();
    //        hcd.setDoCleanup(doCleanUp);
    //        hcd.findCorners();
    //        hcd.showCornerPoints(imp, nmax);

    // ImageProcessor result = hcd.showCornerPoints(imp.getProcessor(), nmax);
    // ImagePlus win = new ImagePlus("Corners from " + imp.getTitle(), result);
    // win.show();
  }

  private ImagePlus drawCorrespondences (List<Correspondence> correspondences, ImagePlus montage) {
    for (int i = 0; i < correspondences.size(); i++) {
      Correspondence c = correspondences.get(i);
      Line line = new Line(c.c0.u, c.c0.v, c.c1.u + montage.getWidth() / 2, c.c1.v);
      line.drawPixels(montage.getProcessor());
    }
    return montage;
  }

  private ImagePlus drawCorrespondences (List<Correspondence> correspondences) {
    if (correspondences.isEmpty()) return null;
    ImageProcessor ip0 = correspondences.get(0).c0.ip;
    ImageProcessor ip1 = correspondences.get(0).c1.ip;
    MontageMaker montageMaker = new MontageMaker();
    ImageStack is = new ImageStack(ip0.getWidth(), ip0.getHeight());
    is.addSlice(ip0);
    is.addSlice(ip1);
    ImagePlus tmp = new ImagePlus("correspondences", is);
    ImagePlus montage = montageMaker.makeMontage2(tmp, 2, 1, 1, 1, 2, 1, 0, false);
    drawCorrespondences(correspondences, montage);
    return montage;
  }

  private Correspondence findCorrespondence (Corner c, List<Corner> corners) {
    double bestDiff = Double.POSITIVE_INFINITY;
    Corner best = corners.get(0);
    for (Corner c2 : corners) {
      double diff = c.difference(c2);
      if (diff < bestDiff) {
        bestDiff = diff;
        best = c2;
      }
    }
//    System.out.println("best score: " + bestDiff);
    return new Correspondence(c, best, bestDiff);
  }

  private boolean showDialog () {
    // display dialog , return false if canceled or on error.
    GenericDialog dlg = new GenericDialog("Harris Corner Detector", IJ.getInstance());
    float def_alpha = HarrisCornerDetector.DEFAULT_ALPHA;
    dlg.addNumericField("Alpha (default: " + def_alpha + ")", alpha, 3);
    int def_threshold = HarrisCornerDetector.DEFAULT_THRESHOLD;
    dlg.addNumericField("Threshold (default: " + def_threshold + ")", threshold, 0);
    dlg.addNumericField("Corners to show (0 = show all)", nmax, 0);
    dlg.addCheckbox("Clean up corners", doCleanUp);
    dlg.showDialog();
    if (dlg.wasCanceled())
      return false;
    if (dlg.invalidNumber()) {
      IJ.showMessage("Error", "Invalid input number");
      return false;
    }
    alpha = (float) dlg.getNextNumber();
    threshold = (int) dlg.getNextNumber();
    nmax = (int) dlg.getNextNumber();
    doCleanUp = dlg.getNextBoolean();
    return true;
  }
}


class Correspondence implements Comparable<Correspondence> {
  Corner c0;
  Corner c1;
  double diff;
  Shift shift;

  public Correspondence (Corner c0, Corner c1, double diff) {
    this.c0 = c0;
    this.c1 = c1;
    this.shift = new Shift(c1.u - c0.u, c1.v - c0.v);
    this.diff = diff;
  }

  public int compareTo (Correspondence c2) {
    if (this.diff < c2.diff) return -1;
    if (this.diff > c2.diff) return 1;
    return 0;
  }
}

class Shift extends Point {
  int acceptError = 4;

  public Shift(int x, int y) {
    super(x, y);
  }

  @Override
  public boolean equals (Object obj) {
    if (obj instanceof Shift) {
      Shift s = (Shift)obj;
      return Math.max(Math.abs(this.x - s.x), Math.abs(this.y - s.y)) <= this.acceptError;
    }
    return super.equals(obj);
  }
}