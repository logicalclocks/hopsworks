/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
