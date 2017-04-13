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
