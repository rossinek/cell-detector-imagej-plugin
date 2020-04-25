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

    public EventsListener() {
      for (int id : WindowManager.getIDList()) {
        ImagePlus imp = WindowManager.getImage(id);
        this.imageOpened(imp);
        // System.out.println("Opened: " + imp.getTitle());
      }
      ImagePlus.addImageListener(this);
      Roi.addRoiListener(this);
    }

    @Override
    public void imageOpened(ImagePlus imp) {
      imp.getCanvas().addMouseListener(this);
      // System.out.println("> IMAGE opened");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // System.out.println("> Mouse released");
      if (this.lastLineRoi != null) {
        RoiObserver.notify(this.lastLineRoi, RoiListener.CREATED);
        this.lastLineRoi = null;
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
          System.out.println("Line mod id: " + id);
          if (id == RoiListener.MODIFIED) {
            this.lastLineRoi = roi;
          }
        } else {
          RoiObserver.notify(roi, id);
        }
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      // ignore
    }

    @Override
    public void mousePressed(MouseEvent e) {
      // ignore
      // System.out.println("> Mouse pressed");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // ignore
      // System.out.println("> Mouse entered");
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // ignore
      // System.out.println("> Mouse exited");
    }

    @Override
    public void imageClosed(ImagePlus imp) {
      // ignore
      // System.out.println("> IMAGE closed");
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
      // ignore
      // System.out.println("> IMAGE updated");
    }
  }
}
