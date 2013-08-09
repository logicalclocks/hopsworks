package se.kth.kthfsdashboard.struct;

import se.kth.kthfsdashboard.utils.FormatUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class MemoryInfo {

   private Long used;
   private Long capacity;

   public MemoryInfo(Long capacity, Long used) {
      this.used = used;
      this.capacity = capacity;
   }

   public long getUsed() {
      return used;
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
      return f.storage(used) + " / " + f.storage(capacity);
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
