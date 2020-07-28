package dev.mtbt.cells;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AbstractCellFrameTest {
  public static Point2D begin;
  public static Point2D end;
  public static List<Point2D> polyline;

  @BeforeAll
  public static void setup() {
    AbstractCellFrameTest.begin = new Point2D.Double(1.0, 1.0);
    AbstractCellFrameTest.end = new Point2D.Double(3.0, 1.0);
    AbstractCellFrameTest.polyline =
        new ArrayList<>(Arrays.asList(AbstractCellFrameTest.begin, new Point2D.Double(1.0, 2.0),
            new Point2D.Double(2.0, 2.0), new Point2D.Double(2.0, 1.0), AbstractCellFrameTest.end));
  }

  private class CellFrameImplementation extends AbstractCellFrame {
    @Override
    public Point2D getBegin() {
      return AbstractCellFrameTest.begin;
    }

    @Override
    public Point2D getEnd() {
      return AbstractCellFrameTest.end;
    }

    @Override
    public List<Point2D> toPolyline() {
      return AbstractCellFrameTest.polyline;
    }

    @Override
    public void fitPolyline(List<Point2D> polyline) {
      AbstractCellFrameTest.polyline = new ArrayList<>(polyline);
    }

    @Override
    public AbstractCellFrame clone() {
      return new CellFrameImplementation();
    }
  }

  @Test
  public void itReturnsProperLength() {
    AbstractCellFrame frame = new CellFrameImplementation();
    assertEquals(frame.getLength(), 4.0);
  }

  @Test
  public void itReturnsNullForPointsFromOutsideTheLine() {
    AbstractCellFrame frame = new CellFrameImplementation();
    assertEquals(frame.pointAlongLine(-0.1), null);
    assertEquals(frame.pointAlongLine(-100), null);
    assertEquals(frame.pointAlongLine(1.1), null);
    assertEquals(frame.pointAlongLine(100), null);
  }

  @Test
  public void itReturnsPointAlongLine() {
    AbstractCellFrame frame = new CellFrameImplementation();
    assertEquals(frame.pointAlongLine(0), AbstractCellFrameTest.begin);
    assertEquals(frame.pointAlongLine(1), AbstractCellFrameTest.end);
    assertEquals(frame.pointAlongLine(0.5), new Point2D.Double(2.0, 2.0));
    assertEquals(frame.pointAlongLine(0.5625), new Point2D.Double(2.0, 1.75));
    assertEquals(frame.pointAlongLine(0.75), new Point2D.Double(2.0, 1.0));
    assertEquals(frame.pointAlongLine(0.75), new Point2D.Double(2.0, 1.0));
  }
}
