package dev.mtbt.bezierTool;

import ij.*;
import ij.gui.*;
import ij.plugin.tool.PlugInTool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * @Author Berin Martini
 * @Version 2012-01-18
 */
public class Bezier_Curve_Tool extends PlugInTool {
  private BezierList bezierList = new BezierList();
  private BezierPoint bezierPoint;
  private int pW = 8; // point width

  public Bezier_Curve_Tool () {
    if (IJ.versionLessThan("1.46f"))
      ;
  }

  public void mouseDragged (ImagePlus imp, MouseEvent e) {
    ImageCanvas ic = imp.getCanvas();
    double x = ic.offScreenXD(e.getX());
    double y = ic.offScreenYD(e.getY());
    if (bezierList.isEmpty())
      bezierList.cursorPos(x, y);
    else {
      if (bezierPoint == null)
        bezierList.dragTo(imp.getOverlay(), x, y);
      else
        bezierPoint.setPoint(x, y);
    }
    updatePointGraphics(imp);
  }

  public void mouseClicked (ImagePlus imp, MouseEvent e) {
    // Once contral points are set then a click off one of the control points ends the plugin.
    if (!bezierList.isEmpty() && !e.isShiftDown()) {
      ImageCanvas ic = imp.getCanvas();
      double x = ic.offScreenXD(e.getX());
      double y = ic.offScreenYD(e.getY());
      bezierPoint = bezierList.insideControlPoint(x, y);
      if (bezierPoint == null) {
        Overlay overlay = imp.getOverlay();
        if (overlay == null)
          return;
        Roi roi = overlay.get(overlay.size() - 1);
        roi = shapeToLine((ShapeRoi) roi);
        imp.setRoi(roi);
        imp.setOverlay(null);
        bezierList = new BezierList();
      }
    }
  }

  private Roi shapeToLine (ShapeRoi roi) {
    Shape shape = roi.getShape();
    PathIterator pIter = shape.getPathIterator(new AffineTransform(), 0.05);
    float[] coords = new float[6];
    Rectangle bounds = roi.getBounds();
    ArrayList<Float> xlist = new ArrayList<Float>();
    ArrayList<Float> ylist = new ArrayList<Float>();
    while (!pIter.isDone()) {
      int segType = pIter.currentSegment(coords);
      xlist.add(new Float(bounds.x + coords[0]));
      ylist.add(new Float(bounds.y + coords[1]));
      pIter.next();
    }
    float[] xpoints = new float[xlist.size()];
    for (int i = 0; i < xlist.size(); i++)
      xpoints[i] = ((Float) xlist.get(i)).floatValue();
    float[] ypoints = new float[ylist.size()];
    for (int i = 0; i < ylist.size(); i++)
      ypoints[i] = ((Float) ylist.get(i)).floatValue();
    return new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.FREELINE);
  }

  public void mousePressed (ImagePlus imp, MouseEvent e) {
    ImageCanvas ic = imp.getCanvas();
    double x = ic.offScreenXD(e.getX());
    double y = ic.offScreenYD(e.getY());
    if (bezierList.isEmpty()) {
      bezierList.setNewBezierPoint(x, y);
      updatePointGraphics(imp);
      return;
    }

    bezierPoint = bezierList.insideControlPoint(x, y);
    if (bezierPoint == null) {
      bezierList.cursorPos(x, y);
      return;
    }

    if (e.isShiftDown()) {
      int pointNum = bezierPoint.getPointNum();
      if (pointNum == 0 || pointNum == 3) {
        bezierPoint = bezierList.clonePoint(bezierPoint);
      }
      updatePointGraphics(imp);
      return;
    }

    if (e.isAltDown()) {
      if (bezierList.onlyOneBezier())
        return;

      int pointNum = bezierPoint.getPointNum();
      if (pointNum == 0 || pointNum == 3) {
        bezierList.removePoint(bezierPoint);
        bezierPoint = null;
        bezierList.cursorPos(x, y);
      }
    }
  }

  public void mouseReleased (ImagePlus imp, MouseEvent e) {
    ImageCanvas ic = imp.getCanvas();
    double x = ic.offScreenXD(e.getX());
    double y = ic.offScreenYD(e.getY());
    if (bezierList.isEmpty()) {
      bezierList.setNewBezierPoint(x, y);
      updatePointGraphics(imp);
    } else {
      if (bezierPoint == null) {
        bezierList.cursorPos(x, y);
        updatePointGraphics(imp);
      }
      bezierPoint = null;
    }
  }

  public String getToolIcon () {
    return "Cf00F0733Fcb33C037P2736455463728291a1b1c2d2d3d4d5d6d7d8d9dadb";
  }

  private void updatePointGraphics (ImagePlus imp) {
    ImageCanvas ic = imp.getCanvas();
    Overlay overlay = new Overlay();
    double[][] coor = bezierList.getPointCoordinates();
    for (int xx = 0; xx < coor.length; xx += 2) {
      double[] point0 = coor[xx];
      double[] point1 = coor[(xx + 1)];
			/*
             * ToenexFix.
			 * Removed because of incorrect use of ic.screenXD projection.
			 * OvalRoi and Line perform this projection.
			 *
			OvalRoi filledOval = new OvalRoi((ic.screenXD(point0[0]) - (pW/2)), (ic.screenYD(point0[1]) - (pW/2)), pW, pW);
			filledOval.setFillColor(Color.red);
			overlay.add(filledOval);
			overlay.add(new OvalRoi((ic.screenXD(point1[0]) - (pW/2)), (ic.screenYD(point1[1]) - (pW/2)), pW, pW));
			overlay.add(new Line(ic.screenXD(point0[0]), ic.screenYD(point0[1]), ic.screenXD(point1[0]), ic.screenYD(point1[1])));
            */

      OvalRoi filledOval = new OvalRoi((point0[0] - (pW / 2)), (point0[1] - (pW / 2)), pW, pW);
      filledOval.setFillColor(Color.red);
      overlay.add(filledOval);
      overlay.add(new OvalRoi((point1[0] - (pW / 2)), (point1[1] - (pW / 2)), pW, pW));
      overlay.add(new Line(point0[0], point0[1], point1[0], point1[1]));
    }
    overlay.setStrokeColor(Color.red);
    addCurve(overlay);
    imp.setOverlay(overlay);
  }

  private void addCurve (Overlay overlay) {
    GeneralPath path = bezierList.getCurve();
    if (path == null)
      return;
    ShapeRoi curveROI = new ShapeRoi(path); // where path is a GeneralPath
    curveROI.setStrokeColor(Color.yellow);
    overlay.add(curveROI);
  }

  class BezierList {
    private Bezier bezierStart = null;
    private Bezier bezierEnd = null;
    private Bezier bezierCurrent = null;

    private int pointSwitch = 0;
    private double x0, y0, x1, y1, x3, y3, xTmp, yTmp;

    public boolean isEmpty () {
      return (bezierStart == null);
    }

    public boolean onlyOneBezier () {
      return (bezierStart == bezierEnd);
    }

    public void setNewBezierPoint (double x, double y) {
      if (pointSwitch == 0) {
        x0 = x;
        y0 = y;
        xTmp = x;
        yTmp = y;
        pointSwitch = 1;
      } else if (pointSwitch == 1) {
        x1 = x;
        y1 = y;
        pointSwitch = 3;
      } else if (pointSwitch == 3) {
        x3 = x;
        y3 = y;
        xTmp = x;
        yTmp = y;
        pointSwitch = 2;
      } else if (pointSwitch == 2) {
        bezierStart = new Bezier(x0, y0, x1, y1, x, y, x3, y3);
        bezierEnd = bezierStart;
        pointSwitch = 0;
      }

    }

    public double[][] getPointCoordinates () {
      double[][] coordinates = {{}};
      if (isEmpty()) {
        if (pointSwitch == 1) {
          double[][] c = {{x0, y0}, {xTmp, yTmp}};
          coordinates = c;
        } else if (pointSwitch == 3) {
          double[][] c = {{x0, y0}, {x1, y1}};
          coordinates = c;
        } else if (pointSwitch == 2) {
          double[][] c = {{x0, y0}, {x1, y1}, {x3, y3}, {xTmp, yTmp}};
          coordinates = c;
        }
      } else {
        coordinates = bezierStart.getPointCoordinates();
        bezierCurrent = bezierStart.next();
        while (bezierCurrent != null) {
          double[][] coor = new double[coordinates.length + 4][2];
          System.arraycopy(coordinates, 0, coor, 0, coordinates.length);
          System.arraycopy(bezierCurrent.getPointCoordinates(), 0, coor, coordinates.length, 4);
          coordinates = coor;
          bezierCurrent = bezierCurrent.next();
        }
      }
      return coordinates;
    }

    public GeneralPath getCurve () {
      if (bezierStart == null)
        return null;
      GeneralPath curve = bezierStart.getCurve();
      bezierCurrent = bezierStart.next();
      while (bezierCurrent != null) {
        curve.append(bezierCurrent.getCurve(), true);
        bezierCurrent = bezierCurrent.next();
      }
      return curve;
    }

    public void removePoint (BezierPoint point) { //Should not be called if only one bezier curve on image
      bezierCurrent = point.getParentBezier();

      if (point.getPointNum() == 3) { // True for the majority of cases

        if (bezierCurrent == bezierStart) {
          bezierStart = bezierCurrent.next();
          bezierStart.point0.movePoint(bezierCurrent.point0.x, bezierCurrent.point0.y);
          bezierStart.point1.movePoint(bezierCurrent.point1.x, bezierCurrent.point1.y);

        } else if (bezierCurrent == bezierEnd) {
          bezierEnd = bezierCurrent.previous();
          bezierEnd.setNext(null);
          bezierEnd.movePoint(3, bezierEnd.point3.x, bezierEnd.point3.y);

        } else {
          Bezier previous = bezierCurrent.previous();
          Bezier next = bezierCurrent.next();
          previous.setNext(next);
          next.setPrevious(previous);

          previous.movePoint(3, previous.point3.x, previous.point3.y);
          previous.movePoint(2, previous.point2.x, previous.point2.y);
        }

      } else if (point.getPointNum() == 0) { //point must belong to bezierStart if it has a pointNum of zero
        bezierStart = bezierCurrent.next();
        bezierStart.setPrevious(null);
        bezierStart.movePoint(0, bezierStart.point0.x, bezierStart.point0.y);

      }

    }

    public BezierPoint clonePoint (BezierPoint oldPoint) {
      Bezier oldBezier = oldPoint.getParentBezier();
      BezierPoint newPoint = null;
      Bezier newBezier = null;

      double[][] coor = oldBezier.getPointCoordinates();
      if (oldPoint.getPointNum() == 3) {
        newBezier = new Bezier(coor[2][0], coor[2][1], (coor[2][0] + coor[2][0] - coor[3][0]), (coor[2][1] + coor[2][1] - coor[3][1]), coor[3][0], coor[3][1], coor[2][0], coor[2][1]);

        oldBezier.insertAsNext(newBezier);
        if (oldBezier.equals(bezierEnd)) {
          bezierEnd = newBezier;
        }
        newPoint = newBezier.point3;
      } else if (oldPoint.getPointNum() == 0) {
        newBezier = new Bezier(coor[0][0], coor[0][1], coor[1][0], coor[1][1], (coor[0][0] + coor[0][0] - coor[1][0]), (coor[0][1] + coor[0][1] - coor[1][1]), coor[0][0], coor[0][1]);
        oldBezier.insertAsPrevious(newBezier);
        if (oldBezier.equals(bezierStart)) {
          bezierStart = newBezier;
        }
        newPoint = newBezier.point0;
      }

      return newPoint;
    }

    public void cursorPos (double x, double y) {
      xTmp = x;
      yTmp = y;
    }

    public void dragTo (Overlay overlay, double x, double y) {
      if (overlay == null)
        return;
      double dx = xTmp - x;
      double dy = yTmp - y;
      overlay.translate((int) dx, (int) dy);

      bezierStart.point0.movePoint((bezierStart.point0.x - dx), (bezierStart.point0.y - dy));
      bezierStart.point1.movePoint((bezierStart.point1.x - dx), (bezierStart.point1.y - dy));
      bezierStart.point2.movePoint((bezierStart.point2.x - dx), (bezierStart.point2.y - dy));
      bezierStart.point3.movePoint((bezierStart.point3.x - dx), (bezierStart.point3.y - dy));

      bezierCurrent = bezierStart.next();
      while (bezierCurrent != null) {
        bezierCurrent.point0.movePoint((bezierCurrent.point0.x - dx), (bezierCurrent.point0.y - dy));
        bezierCurrent.point1.movePoint((bezierCurrent.point1.x - dx), (bezierCurrent.point1.y - dy));
        bezierCurrent.point2.movePoint((bezierCurrent.point2.x - dx), (bezierCurrent.point2.y - dy));
        bezierCurrent.point3.movePoint((bezierCurrent.point3.x - dx), (bezierCurrent.point3.y - dy));
        bezierCurrent = bezierCurrent.next();
      }
      xTmp = x;
      yTmp = y;
    }

    public BezierPoint insideControlPoint (double testX, double testY) {
      //The particular order in which points are checked is relided upon by other parts
      // of the programe, changing this will affect the behaver of other parts of the
      // program in potentialy strage ways.
      BezierPoint controlPoint = bezierStart.insideControlPoint(testX, testY);
      bezierCurrent = bezierStart.next();
      while (bezierCurrent != null && controlPoint == null) {
        controlPoint = bezierCurrent.insideControlPoint(testX, testY);
        bezierCurrent = bezierCurrent.next();
      }
      return controlPoint;
    }


  } // end BezierList

  class Bezier {
    private Bezier next = null;
    private Bezier previous = null;

    public BezierPoint point0;
    public BezierPoint point1;
    public BezierPoint point2;
    public BezierPoint point3;

    Bezier (double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
      point0 = new BezierPoint(this, 0, x0, y0);
      point1 = new BezierPoint(this, 1, x1, y1);
      point2 = new BezierPoint(this, 2, x2, y2);
      point3 = new BezierPoint(this, 3, x3, y3);
    }

    public Bezier next () {
      return next;
    }

    public Bezier previous () {
      return previous;
    }

    public void setPrevious (Bezier newBezier) {
      previous = newBezier;
    }

    public void setNext (Bezier newBezier) {
      next = newBezier;
    }

    public void insertAsNext (Bezier newBezier) {
      newBezier.setNext(next);
      newBezier.setPrevious(this);
      if (next != null)
        next.setPrevious(newBezier);
      next = newBezier;

    }

    public void insertAsPrevious (Bezier newBezier) {
      newBezier.setNext(this);
      newBezier.setPrevious(previous);
      if (previous != null)
        previous.setNext(newBezier);
      previous = newBezier;

    }

    public void movePoint (int pointNum, double nx, double ny) {
      if (pointNum == 0) {
        double newP1X = point1.x - point0.x + nx;
        double newP1Y = point1.y - point0.y + ny;
        point1.movePoint(newP1X, newP1Y);
      } else if (pointNum == 1) {
        if (previous != null) {
          previous.point2.movePoint((point0.x + point0.x - point1.x), (point0.y + point0.y - point1.y));
        }
      } else if (pointNum == 2) {
        if (next != null) {
          next.point1.movePoint((point3.x + point3.x - point2.x), (point3.y + point3.y - point2.y));
        }
      } else if (pointNum == 3) {
        double newP2X = point2.x - point3.x + nx;
        double newP2Y = point2.y - point3.y + ny;
        point2.movePoint(newP2X, newP2Y);
        if (next != null) {
          next.point0.setPoint(nx, ny);
        }
      }
    }

    public double[][] getPointCoordinates () {
      double[][] coordinates = {{point0.x, point0.y}, {point1.x, point1.y}, {point3.x, point3.y}, {point2.x, point2.y}};
      return coordinates;
    }

    public GeneralPath getCurve () {
      GeneralPath curve = new GeneralPath();
      curve.moveTo((float) point0.x, (float) point0.y);
      curve.curveTo((float) point1.x, (float) point1.y, (float) point2.x, (float) point2.y, (float) point3.x, (float) point3.y);
      return curve;
    }

    public BezierPoint insideControlPoint (double testX, double testY) {
      // The order that the points are tested in are very important
      // if they get changed than the behaviour of other parts of
      // the program will get affected.
      if (point1.contains(testX, testY)) {
        return point1;
      } else if (point0.contains(testX, testY)) {
        return point0;
      } else if (point2.contains(testX, testY)) {
        return point2;
      } else if (point3.contains(testX, testY)) {
        return point3;
      }
      return null;
    }

  } //end class Bezier

  class BezierPoint {
    public double x;
    public double y;

    private Bezier bezier;
    private int pointNum; // will be set to 0, 1, 2 or 3 to identify which bezier control point it is.
    private int pW = 8; // point width

    BezierPoint (Bezier bezier, int pointNum, double x, double y) {
      this.bezier = bezier;
      this.pointNum = pointNum;

      movePoint(x, y);
    }

    public void movePoint (double nx, double ny) {
      this.x = nx;
      this.y = ny;
    }

    public void setPoint (double nx, double ny) {
      bezier.movePoint(pointNum, nx, ny);
      movePoint(nx, ny);
    }

    public Bezier getParentBezier () {
      return bezier;
    }

    public int getPointNum () {
      return pointNum;
    }

    public boolean contains (double testX, double testY) {
      Roi pointROI = new Roi(((int) x - (2 * pW)), ((int) y - (2 * pW)), (4 * pW), (4 * pW));
      return (pointROI.contains((int) testX, (int) testY));
    }
  } //end class BezierPoint

}
