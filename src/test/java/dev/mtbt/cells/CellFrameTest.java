package dev.mtbt.cells;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CellFrameTest {
  public static Point2D begin;
  public static Point2D end;
  public static List<Point2D> polyline;

  @BeforeAll
  public static void setup() {
    CellFrameTest.begin = new Point2D.Double(1.0, 1.0);
    CellFrameTest.end = new Point2D.Double(3.0, 1.0);
    CellFrameTest.polyline =
        new ArrayList<>(Arrays.asList(CellFrameTest.begin, new Point2D.Double(1.0, 2.0),
            new Point2D.Double(2.0, 2.0), new Point2D.Double(2.0, 1.0), CellFrameTest.end));
  }

  private class CellFrameImplementation extends CellFrame {
    @Override
    public Point2D getBegin() {
      return CellFrameTest.begin;
    }

    @Override
    public Point2D getEnd() {
      return CellFrameTest.end;
    }

    @Override
    public List<Point2D> toPolyline() {
      return CellFrameTest.polyline;
    }

    @Override
    public void fitPolyline(List<Point2D> polyline) {
      CellFrameTest.polyline = new ArrayList<>(polyline);
    }
  }

  @Test
  public void itReturnsProperLength() {
    CellFrame frame = new CellFrameImplementation();
    assertEquals(frame.getLength(), 4.0);
  }

  @Test
  public void itReturnsNullForPointsFromOutsideTheLine() {
    CellFrame frame = new CellFrameImplementation();
    assertEquals(frame.pointAlongLine(-0.1), null);
    assertEquals(frame.pointAlongLine(-100), null);
    assertEquals(frame.pointAlongLine(1.1), null);
    assertEquals(frame.pointAlongLine(100), null);
  }

  @Test
  public void itReturnsPointAlongLine() {
    CellFrame frame = new CellFrameImplementation();
    assertEquals(frame.pointAlongLine(0), CellFrameTest.begin);
    assertEquals(frame.pointAlongLine(1), CellFrameTest.end);
    assertEquals(frame.pointAlongLine(0.5), new Point2D.Double(2.0, 2.0));
    assertEquals(frame.pointAlongLine(0.5625), new Point2D.Double(2.0, 1.75));
    assertEquals(frame.pointAlongLine(0.75), new Point2D.Double(2.0, 1.0));
    assertEquals(frame.pointAlongLine(0.75), new Point2D.Double(2.0, 1.0));
  }
}
