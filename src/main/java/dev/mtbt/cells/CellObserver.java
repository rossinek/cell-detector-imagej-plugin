package dev.mtbt.cells;

import java.util.ArrayList;
import java.util.List;
import dev.mtbt.imagej.RoiObserver;
import dev.mtbt.imagej.RoiObserverListener;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.ShapeRoi;

public class CellObserver {
  static private final EventsListener eventsListenerInstance = new EventsListener();
  static private final List<CellObserverListener> listeners = new ArrayList<>();

  static public void addListener(CellObserverListener listener) {
    listeners.add(listener);
  }

  static public void removeListener(CellObserverListener listener) {
    listeners.remove(listener);
  }

  static public EventsListener getEventsListenerInstance() {
    return CellObserver.eventsListenerInstance;
  }

  static private void notify(Roi modifiedRoi, int id) {
    new ArrayList<>(listeners).forEach(listener -> {
      if (modifiedRoi.getType() == Roi.LINE && id == RoiListener.CREATED
          && modifiedRoi.getState() == Roi.NORMAL) {
        listener.cellFrameRoisCut((Line) modifiedRoi);
      } else if (modifiedRoi.getType() == Roi.COMPOSITE && id == RoiListener.CREATED) {
        listener.cellFrameRoisShorten((ShapeRoi) modifiedRoi);
      } else if (id == RoiListener.MOVED || id == RoiListener.EXTENDED
          || id == RoiListener.MODIFIED) {
        listener.cellFrameRoiModified((PolygonRoi) modifiedRoi);
      }
    });
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
