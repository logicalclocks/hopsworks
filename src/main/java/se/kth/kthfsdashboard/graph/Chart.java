package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import se.kth.kthfsdashboard.utils.ChartModel;

/**
 *
 * @author Hamidreza Afzali
 */
public class Chart implements Serializable {

    private ChartModel model;
    private String plugin;
    private String pluginInstance;
    private String type;
    private String typeInstance;
    private String ds;
    private String label;
    private String color;
    private String format;

    public Chart() {
        this.model = null;
        this.plugin = null;
        this.pluginInstance = null;
        this.type = null;
        this.typeInstance = null;
        this.ds = null;
        this.label = null;
        this.color = null;
        this.format = null;
    }

    public Chart(ChartModel model, String plugin, String pluginInstance, String type, String typeInstance, String ds, String label, String color, String format) {
        this.model = model;
        this.plugin = plugin;
        this.pluginInstance = pluginInstance;
        this.type = type;
        this.typeInstance = typeInstance;
        this.ds = ds;
        this.label = label;
        this.color = color;
        this.format = format;
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

    public ChartModel getModel() {
        return model;
    }

    public void setModel(ChartModel model) {
        this.model = model;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getPluginInstance() {
        return pluginInstance;
    }

    public void setPluginInstance(String pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeInstance() {
        return typeInstance;
    }

    public void setTypeInstance(String typeInstance) {
        this.typeInstance = typeInstance;
    }

    public String getDs() {
        return ds;
    }

    public void setDs(String ds) {
        this.ds = ds;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
