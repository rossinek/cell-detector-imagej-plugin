package dev.mtbt.cells;

import java.awt.*;

public interface ICellFrame {
  public Point getBegin();

  public Point getEnd();

  public Polygon toPolyline();
}
