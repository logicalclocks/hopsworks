/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.upload;

/**
 * by fanxu123
 */
public class HttpUtils {

  public static boolean isEmpty(String value) {
    return value == null || "".equals(value);
  }

  /**
   * Convert String to long
   * <p>
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
   * <p>
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
