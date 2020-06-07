package dev.mtbt.cells;

import java.util.ArrayList;
import java.util.List;
import dev.mtbt.imagej.RoiObserver;
import dev.mtbt.imagej.RoiObserverListener;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.ShapeRoi;

public class CellObserver {
  // Those are actual IJ tool names
  static public final String TOOL_CUT = "line", TOOL_ERASE = "brush";

  static private final EventsListener eventsListenerInstance = new EventsListener();
  static private final List<CellObserverListener> listeners = new ArrayList<>();

  static private String activeTool = null;

  static public void addListener(CellObserverListener listener) {
    listeners.add(listener);
  }

  static public void removeListener(CellObserverListener listener) {
    listeners.remove(listener);
  }

  static public EventsListener getEventsListenerInstance() {
    return CellObserver.eventsListenerInstance;
  }

  static public void setActiveTool(String tool) {
    if ((activeTool != null || tool != null) && activeTool != tool
        && WindowManager.getCurrentImage() != null) {
      WindowManager.getCurrentImage().deleteRoi();
    }
    if (tool != null) {
      switch (tool) {
        case CellObserver.TOOL_CUT:
          IJ.setTool("line");
          break;
        case CellObserver.TOOL_ERASE:
          IJ.setTool("brush");
          break;
      }
    }
    CellObserver.activeTool = tool;
  }

  static private void notify(Roi modifiedRoi, int id) {
    boolean isCut = modifiedRoi.getType() == Roi.LINE && id == RoiListener.CREATED
        && modifiedRoi.getState() == Roi.NORMAL;
    boolean isErase = modifiedRoi.getType() == Roi.COMPOSITE && id == RoiListener.CREATED;
    boolean potentialModification = !isCut && !isErase && id == RoiListener.MOVED
        || id == RoiListener.EXTENDED || id == RoiListener.MODIFIED;

    for (CellObserverListener listener : new ArrayList<>(listeners)) {
      if (isCut && activeTool == TOOL_CUT) {
        listener.cellFrameRoisCut((Line) modifiedRoi);
      }
      if (isErase && activeTool == TOOL_ERASE) {
        listener.cellFrameRoisShorten((ShapeRoi) modifiedRoi);
      }
      if (potentialModification) {
        listener.cellFrameRoiModified(modifiedRoi);
      }
    }
    // Cleanup after tool was used
    if ((isCut && activeTool == TOOL_CUT) || (isErase && activeTool == TOOL_ERASE)) {
      ImagePlus imp = modifiedRoi.getImage();
      if (imp != null) {
        imp.deleteRoi();
      }
    }
  }

  static private class EventsListener implements RoiObserverListener {
    public EventsListener() {
      RoiObserver.addListener(this);
    }

    @Override
    public void roiModified(Roi modifiedRoi, int id) {
      CellObserver.notify(modifiedRoi, id);
    }
  }
}
