package dev.mtbt;

import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;

public class ImageJUtils {
  public static RoiManager getRoiManager() {
    RoiManager roiManager = RoiManager.getInstance();
    return roiManager == null ? new RoiManager() : roiManager;
  }

  public static void appendResultsTable(ResultsTable target, ResultsTable source) {
    int nColumns = source.getLastColumn() + 1;
    for (int row = 0; row < source.size(); row++) {
      target.incrementCounter();
      for (int column = 0; column < nColumns; column++) {
        double value = source.getValueAsDouble(column, row);
        if (!Double.isNaN(value)) {
          target.addValue(source.getColumnHeading(column), value);
        } else {
          target.addValue(source.getColumnHeading(column), source.getStringValue(column, row));
        }
      }
    }
  }
}
