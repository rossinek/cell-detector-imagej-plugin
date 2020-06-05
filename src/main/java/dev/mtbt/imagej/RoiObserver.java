package dev.mtbt.imagej;

import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class RoiObserver {
  static private final EventsListener eventsListenerInstance = new EventsListener();
  static private final List<RoiObserverListener> listeners = new ArrayList<>();

  static public void addListener(RoiObserverListener listener) {
    listeners.add(listener);
  }

  static public void removeListener(RoiObserverListener listener) {
    listeners.remove(listener);
  }

  static public EventsListener getEventsListenerInstance() {
    return RoiObserver.eventsListenerInstance;
  }

  static private void notify(Roi roi, int id) {
    new ArrayList<>(listeners).forEach(listener -> listener.roiModified(roi, id));
  }

  static private class EventsListener implements RoiListener, MouseListener, ImageListener {
    private Roi lastLineRoi;
    private Roi lastShapeRoi;

    public EventsListener() {
      for (int id : WindowManager.getIDList()) {
        ImagePlus imp = WindowManager.getImage(id);
        this.imageOpened(imp);
      }
      ImagePlus.addImageListener(this);
      Roi.addRoiListener(this);
    }

    @Override
    public void imageOpened(ImagePlus imp) {
      imp.getCanvas().addMouseListener(this);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // fixes bug in ImageJ:
      // the only event for Line roi is of type RoiListener.MODIFIED
      // this code assumes that released mouse after RoiListener.MODIFIED event
      // for Line roi means that constructing Line has been completed
      // and sends fake event of type RoiListener.CREATED
      if (this.lastLineRoi != null) {
        RoiObserver.notify(this.lastLineRoi, RoiListener.CREATED);
        this.lastLineRoi = null;
      }
      if (this.lastShapeRoi != null) {
        RoiObserver.notify(this.lastShapeRoi, RoiListener.CREATED);
        this.lastShapeRoi = null;
      }
    }

    @Override
    public void roiModified(ImagePlus imp, int id) {
      if (imp == null || imp.getRoi() == null) {
        return;
      }
      Roi roi = imp.getRoi();
      if (id == RoiListener.MODIFIED) {
        if (roi.getType() == Roi.LINE) {
          if (id == RoiListener.MODIFIED) {
            this.lastLineRoi = roi;
          }
        } else {
          RoiObserver.notify(roi, id);
        }
      }
      if (roi.getType() == Roi.COMPOSITE && id == RoiListener.CREATED) {
        this.lastShapeRoi = roi;
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      // ignore
    }

    @Override
    public void mousePressed(MouseEvent e) {
      // ignore
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // ignore
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // ignore
    }

    @Override
    public void imageClosed(ImagePlus imp) {
      // ignore
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
      // ignore

    }

    private String roiEventIdAsString(int id) {
      switch (id) {
        case 1:
          return "CREATED";
        case 2:
          return "MOVED";
        case 3:
          return "MODIFIED";
        case 4:
          return "EXTENDED";
        case 5:
          return "COMPLETED";
        case 6:
          return "DELETED";
        default:
          return "UNKNOWN";
      }
    }
  }
}
