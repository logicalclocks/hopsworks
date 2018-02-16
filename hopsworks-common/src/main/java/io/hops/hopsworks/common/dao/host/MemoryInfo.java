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

package io.hops.hopsworks.common.dao.host;

import io.hops.hopsworks.common.util.FormatUtils;
import java.io.Serializable;

public class MemoryInfo implements Serializable {

  private Long used;
  private Long capacity;

  public MemoryInfo() {

  }

  public MemoryInfo(Long capacity, Long used) {
    this.used = used;
    this.capacity = capacity;
  }

  public long getUsed() {
    return used;
  }

  public void setCapacity(Long capacity) {
    this.capacity = capacity;
  }

  public void setUsed(Long used) {
    this.used = used;
  }

  public long getCapacity() {
    return capacity;
  }

  private double usagePercentage() {
    if (used == null || capacity == null) {
      return 0;
    }
    return ((double) used) / capacity * 100d;
  }

  public String getUsagePercentageString() {

    return String.format("%1.1f", usagePercentage()) + "%";
  }

  public String getUsageInfo() {
    FormatUtils f = new FormatUtils();
    if (used == null || capacity == null) {
      return "N/A";
    }
    return FormatUtils.storage(used) + " / " + FormatUtils.storage(capacity);
  }

  public String getPriority() {
    if (usagePercentage() > 75) {
      return "priorityHigh";
    } else if (usagePercentage() > 25) {
      return "priorityMed";
    }
    return "priorityLow";
  }
}
