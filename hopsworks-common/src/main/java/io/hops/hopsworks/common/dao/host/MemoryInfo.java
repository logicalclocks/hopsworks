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
