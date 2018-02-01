/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

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
