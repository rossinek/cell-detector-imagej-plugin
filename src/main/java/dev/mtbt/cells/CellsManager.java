package dev.mtbt.cells;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import dev.mtbt.imagej.ImageJUtils;
import dev.mtbt.imagej.RoiObserver;
import dev.mtbt.imagej.IRoiObserverListener;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.gui.ShapeRoi;
import ij.plugin.frame.RoiManager;

public class CellsManager implements IRoiObserverListener, ListDataListener {
  // Those are actual IJ tool names - it shouldn't be changed
  static public final String TOOL_CUT = "line", TOOL_ERASE = "brush";

  static private String activeTool = null;

  private ImagePlus observedImage;
  private CellCollection cellCollection;
  private int lastDisplayedRoisFrame = -1;
  private Set<Roi> lastDisplayedRois = new HashSet<>();

  public CellsManager(ImagePlus observedImage, CellCollection cellCollection) {
    this.observedImage = observedImage;
    this.cellCollection = cellCollection;
    RoiObserver.addListener(this);
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

  public void displayCells() {
    this.displayCells(false);
  }

  public void displayCells(boolean showEndpoints) {
    RoiManager roiManager = this.getObservedRoiManager();
    roiManager.reset();
    roiManager.runCommand("show all with labels");
    roiManager.runCommand("usenames", "true");

    List<Cell> currentCells = this.getCurrentCells();
    int frame = this.observedImage.getT();
    currentCells.stream().forEach(cell -> roiManager.addRoi(cell.getObservedRoi(frame)));
    if (showEndpoints) {
      currentCells.stream()
          .forEach(cell -> cell.endsToRois(frame).forEach(roi -> roiManager.addRoi(roi)));
    }
    this.lastDisplayedRoisFrame = frame;
    this.lastDisplayedRois = new HashSet<Roi>(Arrays.asList(roiManager.getRoisAsArray()));
  }

  @Override
  public void roiModified(Roi modifiedRoi, int id) {
    if (this.observedImage != null && modifiedRoi.getImage() == this.observedImage) {
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

  public List<Cell> getCurrentCells() {
    int frame = this.observedImage.getT();
    return cellCollection.getCells(frame);
  }

  private void notify(Roi modifiedRoi, int id) {
    boolean isCut = modifiedRoi.getType() == Roi.LINE && id == RoiListener.CREATED
        && modifiedRoi.getState() == Roi.NORMAL;
    boolean isErase = modifiedRoi.getType() == Roi.COMPOSITE && id == RoiListener.CREATED;
    boolean potentialModification = !isCut && !isErase && id == RoiListener.MOVED
        || id == RoiListener.EXTENDED || id == RoiListener.MODIFIED;

    List<Cell> cells = this.getCurrentCells();

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
        case CellsManager.TOOL_CUT:
          IJ.setTool(CellsManager.TOOL_CUT);
          break;
        case CellsManager.TOOL_ERASE:
          IJ.setTool(CellsManager.TOOL_ERASE);
          break;
      }
    }
    CellsManager.activeTool = tool;
  }

  @Override
  public void intervalRemoved(ListDataEvent e) {
    RoiManager roiManager = RoiManager.getInstance();
    if (roiManager != null && this.lastDisplayedRoisFrame == this.observedImage.getT()) {
      // check if any of previously displayed cells
      // that should be still visible is missing
      List<Cell> shouldStillBeThere = this.getCurrentCells().stream()
          .filter(cell -> this.lastDisplayedRois.stream().anyMatch(cell::isOwnCellFrameRoi))
          .collect(Collectors.toList());
      shouldStillBeThere.forEach(cell -> {
        if (Arrays.asList(roiManager.getRoisAsArray()).stream()
            .noneMatch(cell::isOwnCellFrameRoi)) {
          cell.clearFuture(this.observedImage.getT());
        }
      });
    }
  }

  @Override
  public void intervalAdded(ListDataEvent e) {
  }

  @Override
  public void contentsChanged(ListDataEvent e) {
  }
}
