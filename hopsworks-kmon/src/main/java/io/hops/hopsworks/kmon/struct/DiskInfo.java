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

package io.hops.hopsworks.kmon.struct;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;

public class DiskInfo implements Serializable {

  private double used;
  private double free;
  private String disk;
  private String values;
  private DecimalFormat format = new DecimalFormat("#.##");

  public DiskInfo(String disk, String values) throws ParseException {
    this.disk = disk;
    this.values = values;
    String valuesArray[] = values.split("[\\[,]");
    this.used = format.parse(valuesArray[1]).doubleValue();
    this.free = format.parse(valuesArray[2]).doubleValue();
  }

  public String getUsed() {
    return formatSize(used);
  }

  public String getFree() {
    return formatSize(free);
  }

  public String getTotal() {
    return formatSize(used + free);
  }

  public String getDisk() {
    return "/" + disk.replace("-", "/");
  }

  public String getDiskSimpleName() {
    return disk;
  }

  public int getUsedPercent() {

    return ((Long) (Math.round(used / (used + free) * 100))).intValue();
  }

  public String getValues() {
    return values;
  }

  private String formatSize(double s) {

    String dim = "B";
    double size = s;
    if (size > 1024) {
      size = size / 1024;
      dim = "KB";
      if (size > 1024) {
        size = size / 1024;
        dim = "MB";
        if (size > 1024) {
          size = size / 1024;
          dim = "GB";
          if (size > 1024) {
            size = size / 1024;
            dim = "TB";
          }
        }
      }
    }
    return format.format(size) + " " + dim;
  }
}
