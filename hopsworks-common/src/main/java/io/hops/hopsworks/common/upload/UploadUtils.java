/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.upload;

import com.google.common.base.Strings;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UploadUtils {
  private static final Logger LOGGER = Logger.getLogger(UploadUtils.class.getName());
  
  public static boolean isEmpty(String value) {
    return Strings.isNullOrEmpty(value);
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
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      LOGGER.log(Level.WARNING, "Failed to parse {0} to long. {1}", new Object[]{value, e.getMessage()});
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
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      LOGGER.log(Level.WARNING, "Failed to parse {0} to int. {1}", new Object[]{value, e.getMessage()});
      return def;
    }
  }
}
