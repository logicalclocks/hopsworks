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

package io.hops.hopsworks.common.upload;

public class HttpUtils {

  public static boolean isEmpty(String value) {
    return value == null || "".equals(value);
  }

  /**
   * Convert String to long
   * <p/>
   * @param value
   * @param def default value
   * @return
   */
  public static long toLong(String value, long def) {
    if (isEmpty(value)) {
      return def;
    }

    try {
      return Long.valueOf(value);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return def;
    }
  }

  /**
   * Convert String to int
   * <p/>
   * @param value
   * @param def default value
   * @return
   */
  public static int toInt(String value, int def) {
    if (isEmpty(value)) {
      return def;
    }
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return def;
    }
  }
}
