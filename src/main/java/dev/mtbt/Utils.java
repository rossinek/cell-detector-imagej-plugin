package dev.mtbt;

import ij.plugin.frame.RoiManager;

public class Utils {
  public static RoiManager getRoiManager () {
    RoiManager roiManager = RoiManager.getInstance();
    return roiManager == null ? new RoiManager() : roiManager;
  }
}
