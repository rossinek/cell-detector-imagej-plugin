package dev.mtbt.cells;

import dev.mtbt.imagej.RoiObserver;
import dev.mtbt.imagej.RoiObserverListener;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.ShapeRoi;

public class CellObserver implements RoiObserverListener {
  // Those are actual IJ tool names
  static public final String TOOL_CUT = "line", TOOL_ERASE = "brush";

  static private String activeTool = null;


  private CellObserverListener listener;

  public CellObserver(CellObserverListener listener) {
    this.listener = listener;
    RoiObserver.addListener(this);
  }

  @Override
  public void roiModified(Roi modifiedRoi, int id) {
    if (this.listener.getObservedImage() != null
        && modifiedRoi.getImage() == this.listener.getObservedImage()) {
      this.notify(modifiedRoi, id);
    }
  }

  private void notify(Roi modifiedRoi, int id) {
    boolean isCut = modifiedRoi.getType() == Roi.LINE && id == RoiListener.CREATED
        && modifiedRoi.getState() == Roi.NORMAL;
    boolean isErase = modifiedRoi.getType() == Roi.COMPOSITE && id == RoiListener.CREATED;
    boolean potentialModification = !isCut && !isErase && id == RoiListener.MOVED
        || id == RoiListener.EXTENDED || id == RoiListener.MODIFIED;

    if (isCut && activeTool == TOOL_CUT) {
      this.listener.cellFrameRoisCut((Line) modifiedRoi);
    }
    if (isErase && activeTool == TOOL_ERASE) {
      this.listener.cellFrameRoisShorten((ShapeRoi) modifiedRoi);
    }
    if (potentialModification) {
      this.listener.cellFrameRoiModified(modifiedRoi);
    }
    // Cleanup after tool was used
    if ((isCut && activeTool == TOOL_CUT) || (isErase && activeTool == TOOL_ERASE)) {
      ImagePlus imp = modifiedRoi.getImage();
      if (imp != null) {
        imp.deleteRoi();
      }
    }
  }

  public void destroy() {
    RoiObserver.removeListener(this);
  }

  static public void setActiveTool(String tool) {
    if ((activeTool != null || tool != null) && activeTool != tool
        && WindowManager.getCurrentImage() != null) {
      WindowManager.getCurrentImage().deleteRoi();
    }
    if (tool != null) {
      switch (tool) {
        case CellObserver.TOOL_CUT:
          IJ.setTool(CellObserver.TOOL_CUT);
          break;
        case CellObserver.TOOL_ERASE:
          IJ.setTool(CellObserver.TOOL_ERASE);
          break;
      }
    }
    CellObserver.activeTool = tool;
  }
}
