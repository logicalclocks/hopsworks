package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import org.primefaces.context.RequestContext;
import se.kth.kthfsdashboard.struct.CollectdBasicType;
import se.kth.kthfsdashboard.struct.CollectdPluginInstance;
import se.kth.kthfsdashboard.struct.CollectdPluginType;
import se.kth.kthfsdashboard.struct.ColorType;
import se.kth.kthfsdashboard.struct.ServiceType;
import se.kth.kthfsdashboard.utils.ChartModel;
import se.kth.kthfsdashboard.utils.CollectdConfigUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@SessionScoped
public class NewGraphController implements Serializable {

    @EJB
    private GraphEJB graphEjb;
    @ManagedProperty(value = "#{EditGraphController}")
    private EditGraphsController editGraphController;
    private static List<String> types;
    private static List<String> typeInstances;
    private List<String> groups;
    private List<String> plugins;
    private List<String> pluginInstances;
    private boolean validId;
    private Graph graph;
    private List<Chart> charts;
    private Chart chart = new Chart();
    private String typeInstanceInfo;
    private static Map<String, CollectdPluginInstance> dbiPluginInstances;
    private static Map<String, HashMap<String, CollectdPluginInstance>> jmxPluginInstances;
    private static final String COLOR_N = "COLOR(@n)";     /// ???????????????????
    private static final String VARIABLE = "@n";
    private static final String COLLECTD_PLUGIN_FILE = "/etc/collectd/collectd.conf";
    private static final String COLLECTD_DBI_PLUGIN_FILE = "/home/x/NetBeansProjects/kthfs-pantry/cookbooks/collect/templates/default/dbi_plugin.conf.erb";
    private static final String COLLECTD_JMX_PLUGIN_FILE = "/home/x/NetBeansProjects/kthfs-pantry/cookbooks/collect/templates/default/jmx_plugin.conf.erb";
    private static final Logger logger = Logger.getLogger(NewGraphController.class.getName());

    public NewGraphController() {
    }

    @PostConstruct
    public void init() {
        logger.info("init NewGraphsController");
        dbiPluginInstances = CollectdConfigUtils.parseDbiPlugin(COLLECTD_DBI_PLUGIN_FILE);
        jmxPluginInstances = CollectdConfigUtils.parseJMXPlugin(COLLECTD_JMX_PLUGIN_FILE);
    }

    public List<String> getGroups() {
        return groups;
    }

    public void checkGraphId() {
        validId = !graphEjb.exists(graph.getGraphId());
    }

    public EditGraphsController getEditGraphController() {
        return editGraphController;
    }

    public void setEditGraphController(EditGraphsController editGraphController) {
        this.editGraphController = editGraphController;
    }

    public List<Chart> getCharts() {
        return charts;
    }

    public void setCharts(List<Chart> charts) {
        this.charts = charts;
    }

    public Chart getChart() {
        return chart;
    }

    public void setChart(Chart chart) {
        this.chart = chart;
    }

    public boolean isIdValid() {
        return validId;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public List<String> getPluginInstances() {
        return pluginInstances;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public List<String> getTypeInstances() {
        return typeInstances;
    }

    public void showNewGraphDialog() {
        graph = new Graph();
        graph.setGraphId("testId"); // remove this
        charts = new ArrayList<Chart>();
        groups = null;
        plugins = null;
        pluginInstances = null;
        // TODO This can be removed. It is also loaded in init
        dbiPluginInstances = CollectdConfigUtils.parseDbiPlugin(COLLECTD_DBI_PLUGIN_FILE);
        jmxPluginInstances = CollectdConfigUtils.parseJMXPlugin(COLLECTD_JMX_PLUGIN_FILE);
        RequestContext.getCurrentInstance().update("formNew");
        RequestContext.getCurrentInstance().reset("formNew");
        RequestContext.getCurrentInstance().execute("dlgNewGraph.show()");
    }

    public void showNewChartDialog() {
        chart = new Chart();
        RequestContext.getCurrentInstance().update("formNewChart");
        RequestContext.getCurrentInstance().execute("dlgNewChart.show()");
    }

    public void changedPluginInstance() {
        for (Chart c : charts) {
            c.setPluginInstance(graph.getPluginInstance());
        }
    }

    public void addGraph() {
        groups = graphEjb.findGroups(graph.getTarget());
        Integer groupRank;
        if (groups.contains(graph.getGroup())) {
            groupRank = graphEjb.groupRank(graph.getTarget(), graph.getGroup());
        } else {
            groupRank = graphEjb.lastGroupRank(graph.getTarget());
            groupRank = groupRank == null ? 0 : groupRank + 1;
        }
        Integer rank = graphEjb.lastRank(graph.getTarget(), graph.getGroup());
        rank = rank == null ? 0 : rank + 1;

        graph.setGroupRank(groupRank);
        graph.setRank(rank);
        graphEjb.persistGraph(graph);
        editGraphController = new EditGraphsController();
        editGraphController.setSelectedGraph(graph);
        logger.log(Level.INFO, "Graph added");
    }

    public void addChart() {
        chart.setPlugin(graph.getPlugin());
        chart.setPluginInstance(graph.getPluginInstance());
        charts.add(chart);
        RequestContext.getCurrentInstance().execute("dlgNewChart.hide()");
    }

    public void loadGroupsAndPlugins() {
        plugins = new ArrayList<String>();
        groups = graphEjb.findGroups(graph.getTarget());
        if (graph.ifTargets(ServiceType.KTHFS.toString())
                || graph.ifTargets(ServiceType.YARN.toString())
                || graph.ifTargets(ServiceType.Spark.toString())
                || graph.ifTargets(ServiceType.MapReduce.toString())) {
            plugins.add(CollectdPluginType.GenericJMX.toString());

        } else if (graph.ifTargets(ServiceType.MySQLCluster.toString())) {
            plugins.add(CollectdPluginType.dbi.toString());
        } else {
            plugins.addAll(CollectdConfigUtils.loadPluginsFromConfigFile(COLLECTD_PLUGIN_FILE));
        }

        if (!groups.isEmpty()) {
            graph.setGroup(groups.get(0));
        } else {
            graph.setGroup(null);
        }
        if (!plugins.isEmpty()) {
            graph.setPlugin(plugins.get(0));
        } else {
            graph.setPlugin(null);
        }
        loadPluginInstance();
    }

    public void loadPluginInstance() {        
        pluginInstances = new ArrayList<String>();
        if (!plugins.isEmpty() && isDbi()) {
            for (String instance : dbiPluginInstances.keySet()) {
                pluginInstances.add(instance);
            }
        } else if (!plugins.isEmpty() && isGenericJMX()) {
            for (String target : jmxPluginInstances.keySet()) {
                if (target.startsWith(graph.getTarget())) {
                    for (String instance : jmxPluginInstances.get(target).keySet()) {
                        pluginInstances.add(instance);
                    }
                }
            }
        }
        if (!pluginInstances.isEmpty()) {
            graph.setPluginInstance(pluginInstances.get(0));
        } else {
            graph.setPluginInstance(null);
        }
    }

    private List<String> loadBasicTypes() {
        List<String> basicTypes = new ArrayList<String>();
        for (CollectdBasicType t : CollectdBasicType.values()) {
            basicTypes.add(t.toString());
        }
        return basicTypes;
    }

    public List<String> getTypes() {

        if (graph == null || graph.getTarget() == null || graph.getPluginInstance() == null) {
            return null;
        }
        if (!plugins.isEmpty() && isDbi()) {
            if (dbiPluginInstances == null) {
                dbiPluginInstances = CollectdConfigUtils.parseDbiPlugin(COLLECTD_DBI_PLUGIN_FILE);
            }
            types = dbiPluginInstances.get(graph.getPluginInstance()).getTypes();

        } else {
            types = loadBasicTypes();
        }
        if (!types.isEmpty()) {
            chart.setType(types.get(0));
        } else {
            chart.setType(null);
        }
        loadTypeInstances();
        return types;
    }

    public void loadTypeInstances() {
        typeInstances = new ArrayList<String>();
        if (!plugins.isEmpty() && isDbi()) {
            for (String instance : dbiPluginInstances.get(graph.getPluginInstance()).getTypeInstances(chart.getType())) {
                typeInstances.add(instance);
            }
        }
        if (!typeInstances.isEmpty()) {
            chart.setTypeInstance(typeInstances.get(0));
            if (isDbi()) {
                chart.setDs("value");
            }
            if (isTypeInstanceVariable()) {
                chart.setColor(COLOR_N);
                chart.setModel(ChartModel.LINES);
            } else {
                chart.setColor(ColorType.values()[0].toString());
                chart.setModel(ChartModel.LINE);
            }
        } else {
            chart.setType(null);
            chart.setModel(ChartModel.LINE);
        }
    }

    public String getTypeInstanceInfo() {
        typeInstanceInfo = "";
        if (graph != null && chart != null && isDbi()) {
            typeInstanceInfo = dbiPluginInstances.get(graph.getPluginInstance()).getInfo(chart.getType(), chart.getTypeInstance());
        }
        return typeInstanceInfo;
    }

    private boolean isDbi() {
        return graph.getPlugin().equals(CollectdPluginType.dbi.toString());
    }

    private boolean isGenericJMX() {
        return graph.getPlugin().equals(CollectdPluginType.GenericJMX.toString());
    }

    private boolean isTypeInstanceVariable() {
        return chart.getTypeInstance().contains(VARIABLE);
    }
}