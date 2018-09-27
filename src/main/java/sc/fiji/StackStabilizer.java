package sc.fiji;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Developement>Stabilize stack")
public class StackStabilizer implements Command {

    @Parameter
    private ImagePlus imp;

    static float alpha = HarrisCornerDetector.DEFAULT_ALPHA;
    static int threshold = HarrisCornerDetector.DEFAULT_THRESHOLD;
    static int nmax = 0;	//points to show
    static boolean doCleanUp = true;

    /**
     * Main plugin method invoked by ImageJ.
     */
    @Override
    public void run() {
        if (!showDialog()) return; //dialog canceled or error
        HarrisCornerDetector hcd = new HarrisCornerDetector(imp.getProcessor(), alpha, threshold);
        hcd.setDoCleanup(doCleanUp);
        hcd.findCorners();
        ImageProcessor result = hcd.showCornerPoints(imp.getProcessor(), nmax);
        ImagePlus win = new ImagePlus("Corners from " + imp.getTitle(), result);
        win.show();
    }

    private boolean showDialog() {
        // display dialog , return false if canceled or on error.
        GenericDialog dlg = new GenericDialog("Harris Corner Detector", IJ.getInstance());
        float def_alpha = HarrisCornerDetector.DEFAULT_ALPHA;
        dlg.addNumericField("Alpha (default: "+def_alpha+")", alpha, 3);
        int def_threshold = HarrisCornerDetector.DEFAULT_THRESHOLD;
        dlg.addNumericField("Threshold (default: "+def_threshold+")", threshold, 0);
        dlg.addNumericField("Corners to show (0 = show all)", nmax, 0);
        dlg.addCheckbox("Clean up corners", doCleanUp);
        dlg.showDialog();
        if(dlg.wasCanceled())
            return false;
        if(dlg.invalidNumber()) {
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