package se.kth.kthfsdashboard.struct;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
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
        return formatSize(used+free);
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
