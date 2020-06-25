package dev.mtbt.cells;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import dev.mtbt.ImageJUtils;
import dev.mtbt.imagej.RoiObserver;
import dev.mtbt.imagej.RoiObserverListener;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.ShapeRoi;
import ij.plugin.frame.RoiManager;

public class CellObserver implements RoiObserverListener, ListDataListener {
  // Those are actual IJ tool names
  static public final String TOOL_CUT = "line", TOOL_ERASE = "brush";

  static private String activeTool = null;

  private CellObserverListener listener;

  public CellObserver(CellObserverListener listener) {
    this.listener = listener;
    RoiObserver.addListener(this);
    RoiManager roiManager = RoiManager.getInstance();
    if (roiManager != null) {

    }
  }

  public RoiManager getObservedRoiManager() {
    RoiManager roiManager = ImageJUtils.getRoiManager();
    try {
      javax.swing.JList list = (javax.swing.JList) ((javax.swing.JScrollPane) ImageJUtils
          .getRoiManager().getComponents()[0]).getViewport().getView();
      // add self as list data listener but ensure that is added only once
      list.getModel().removeListDataListener(this);
      list.getModel().addListDataListener(this);
    } catch (Exception e) {
      System.out.println(
          "Implementation of RoiManager has been changed – cells plugin plugin needs adjustments.");
    }
    return roiManager;
  }

  @Override
  public void roiModified(Roi modifiedRoi, int id) {
    if (this.listener.getObservedImage() != null
        && modifiedRoi.getImage() == this.listener.getObservedImage()) {
      this.notify(modifiedRoi, id);
    }
  }

  private List<PolygonRoi> getCellsActiveRois(Cell cell) {
    RoiManager roiManager = RoiManager.getInstance();
    if (roiManager != null) {
      return Arrays.asList(roiManager.getRoisAsArray()).stream().filter(cell::isOwnCellFrameRoi)
          .map(roi -> (PolygonRoi) roi).collect(Collectors.toList());
    }
    return new ArrayList<PolygonRoi>();
  }

  private void notify(Roi modifiedRoi, int id) {
    boolean isCut = modifiedRoi.getType() == Roi.LINE && id == RoiListener.CREATED
        && modifiedRoi.getState() == Roi.NORMAL;
    boolean isErase = modifiedRoi.getType() == Roi.COMPOSITE && id == RoiListener.CREATED;
    boolean potentialModification = !isCut && !isErase && id == RoiListener.MOVED
        || id == RoiListener.EXTENDED || id == RoiListener.MODIFIED;

    List<Cell> cells = listener.getActiveObservedCells();

    if (isCut && activeTool == TOOL_CUT) {
      cells.forEach(cell -> {
        cell.cellFrameRoisCut(this.getCellsActiveRois(cell), (Line) modifiedRoi);
      });
    }
    if (isErase && activeTool == TOOL_ERASE) {
      cells.forEach(cell -> {
        cell.cellFrameRoisShorten(this.getCellsActiveRois(cell), (ShapeRoi) modifiedRoi);
      });
    }
    if (potentialModification) {
      cells.forEach(cell -> {
        if (cell.isOwnCellFrameRoi(modifiedRoi)) {
          cell.cellFrameRoiModified(modifiedRoi);
        }
      });
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

  @Override
  public void intervalRemoved(ListDataEvent e) {
    System.out.println("> intervalRemoved");
    RoiManager roiManager = RoiManager.getInstance();
    if (roiManager != null) {
      Roi[] rois = roiManager.getRoisAsArray();
      this.listener.getActiveObservedCells().forEach(cell -> {
        if (Arrays.asList(rois).stream().noneMatch(cell::isOwnCellFrameRoi)) {
          // cell.clearFuture(listener.getObservedImage().getT());
          System.out.println("Current rois: " + Arrays.asList(rois).stream().map(r -> r.getName())
              .collect(Collectors.joining(", ")));
          System.out.println("Should be removed! " + cell.getName());
        }
      });
    }
  }

  @Override
  public void intervalAdded(ListDataEvent e) {
  }

  @Override
  public void contentsChanged(ListDataEvent e) {
    System.out.println("> contentsChanged");
  }
}
