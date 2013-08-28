package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import se.kth.kthfsdashboard.utils.ChartType;

/**
 *
 * @author Hamidreza Afzali
 */
public class Chart implements Serializable{

    private final ChartType model;
    private final String plugin;
    private final String pluginInstance;
    private final String type;
    private final String typeInstance;
    private final String ds;
    private final String label;
    private final String color;
    private final String format;

    public Chart(ChartType model, String plugin, String pluginInstance, String type, String typeInstance, String ds, String label, String color, String format) {
        this.model = model;
        this.pluginInstance = pluginInstance;
        this.type = type;
        this.typeInstance = typeInstance;
        this.ds = ds;
        this.label = label;
        this.color = color;
        this.format = format;
        this.plugin = plugin;
    }

    public ChartType getModel() {
        return model;
    }

    public String getType() {
        return type;
    }

    public String getTypeInstance() {
        return typeInstance;
    }

    public String getDs() {
        return ds;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return "Chart: {model: " + model + ", type: " + type + ", type_instance: " + typeInstance
                + ", ds: " + ds + ", label: " + label + ", color:" + color + ", format:" + format;
    }

    public String getRrdFileName() {
        String rrdFile = plugin;
        if (pluginInstance != null && !pluginInstance.equals("")) {
            rrdFile += "-" + pluginInstance;
        }
        rrdFile += "/" + type;
        if (typeInstance != null && !typeInstance.equals("")) {
            rrdFile += "-" + typeInstance;
        }
        rrdFile += ".rrd";
        return rrdFile;
    }
}
