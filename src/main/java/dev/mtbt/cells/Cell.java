package dev.mtbt.cells;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatProcessor;

import java.awt.*;

public class Cell {
  private Polygon spine;

  public static Cell detectCell (FloatProcessor fp, Point initialPoint) {
    int[] xPoints = { initialPoint.x, initialPoint.x + 20 };
    int[] yPoints = { initialPoint.y, initialPoint.y + 20 };
    return new Cell(new Polygon(xPoints, yPoints, 2));
  }

  public Cell (Polygon spine) {
    this.spine = spine;
  }

  public PolygonRoi toRoi () {
    return new PolygonRoi(spine, Roi.POLYLINE);
  }
}
