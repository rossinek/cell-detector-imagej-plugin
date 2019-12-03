package dev.mtbt.util;

import java.util.HashMap;
import java.util.Map;

public class Performance {
  private static Map<String, Long> starts = new HashMap<>();

  public static void start(String key) {
    starts.put(key, System.currentTimeMillis());
  }

  public static long end(String key) {
    Long start = starts.remove(key);
    long end = System.currentTimeMillis();
    if (start != null) {
      long time = end - start;
      System.out.println(key + ": " + time + " ms");
      return time;
    }
    return 0;
  }
}
