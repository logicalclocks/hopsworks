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

  public static String getHost(String url) {
    if (url == null || url.length() == 0) {
      return "";
    }

    int doubleslash = url.indexOf("//");
    if (doubleslash == -1) {
      doubleslash = 0;
    } else {
      doubleslash += 2;
    }

    int end = url.indexOf('/', doubleslash);
    end = end >= 0 ? end : url.length();

    int port = url.indexOf(':', doubleslash);
    end = (port > 0 && port < end) ? port : end;

    return url.substring(doubleslash, end);
  }

}
