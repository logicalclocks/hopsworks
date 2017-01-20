package io.hops.hopsworks.common.util;

public class Ip {

  public static boolean validIp(String ip) {
    try {
      if (ip == null || ip.isEmpty()) {
        return false;
      }

      String[] parts = ip.split("\\.");
      if (parts.length != 4) {
        return false;
      }

      for (String s : parts) {
        int i = Integer.parseInt(s);
        if ((i < 0) || (i > 255)) {
          return false;
        }
      }
      if (ip.endsWith(".")) {
        return false;
      }

      return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

}
