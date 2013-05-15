package se.kth.kthfsdashboard.struct;

import se.kth.kthfsdashboard.util.Formatter;

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

    private double getUsagePercentage() {

        return ((double) used) / capacity * 100d;
    }

    public String getUsagePercentageString() {

        return String.format("%1.1f", getUsagePercentage()) + "%";
    }

    public String getUsageInfo() {
        Formatter f = new Formatter();

        return f.storage(used) + " / " + f.storage(capacity);
    }

    public String getPriority() {

        if (getUsagePercentage() > 75) {
            return "priorityHigh";
        } else if (getUsagePercentage() > 25) {
            return "priorityMed";
        }
        return "priorityLow";
    }
}
