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
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.context.RequestContext;
import se.kth.kthfsdashboard.struct.CollectdBasicType;
import se.kth.kthfsdashboard.struct.CollectdPluginInstance;
import se.kth.kthfsdashboard.struct.CollectdPluginType;
import se.kth.kthfsdashboard.struct.ColorType;
import se.kth.kthfsdashboard.struct.ServiceType;
import se.kth.kthfsdashboard.struct.ChartModel;
import se.kth.kthfsdashboard.struct.RoleType;
import se.kth.kthfsdashboard.utils.CollectdConfigUtils;
import se.kth.kthfsdashboard.utils.ColorUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@SessionScoped
public class GraphControllerNew implements Serializable {

    @EJB
    private GraphEJB graphEjb;
    private static List<String> types;
    private static List<String> typeInstances;
    private List<String> groups;
    private List<String> plugins;
    private List<String> pluginInstances;
    private boolean validId;
    private Graph graph;
    private Chart chart = new Chart();
    private String typeInstanceInfo;
    private Map<String, CollectdPluginInstance> dbiPluginInstances;
    private Map<String, HashMap<String, CollectdPluginInstance>> jmxPluginInstances;
    private static final String COLLECTD_PLUGIN_FILE = "/etc/collectd/collectd.conf";
    private static final String COLLECTD_DBI_PLUGIN_FILE = "/etc/collectd/plugins/dbi_plugin.conf";
    private static final String COLLECTD_JMX_PLUGIN_FILE = "/etc/collectd/plugins/jmx_plugin.conf";
    private static final Logger logger = Logger.getLogger(GraphControllerNew.class.getName());
    private Map<String, String> pluginInstanceTargetMap;
    private static final String VAR_N = "@n";

    public GraphControllerNew() {
    }

    @PostConstruct
    public void init() {
        logger.info("init GraphControllerNew");
        dbiPluginInstances = CollectdConfigUtils.parseDbiPlugin(COLLECTD_DBI_PLUGIN_FILE);
        jmxPluginInstances = CollectdConfigUtils.parseJMXPlugin(COLLECTD_JMX_PLUGIN_FILE);
    }

    public List<String> getGroups() {
        return groups;
    }

    public void checkGraphId() {
        validId = !graphEjb.exists(graph.getGraphId());
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
        // TODO This can be removed. It is also loaded in init
        dbiPluginInstances = CollectdConfigUtils.parseDbiPlugin(COLLECTD_DBI_PLUGIN_FILE);
        jmxPluginInstances = CollectdConfigUtils.parseJMXPlugin(COLLECTD_JMX_PLUGIN_FILE);

        graph = new Graph();
//        graph.setGraphId("testId"); // remove this
        groups = null;
        plugins = null;
        pluginInstances = null;
        RequestContext.getCurrentInstance().update("formNew");
        RequestContext.getCurrentInstance().reset("formNew");
        RequestContext.getCurrentInstance().execute("dlgNewGraph.show()");
    }

    public void showNewChartDialog() {
        chart = new Chart();
        RequestContext.getCurrentInstance().update("formNewChart");
        RequestContext.getCurrentInstance().execute("dlgNewChart.show()");
    }

    public void addGraph(ActionEvent actionEvent) {

        if (graph.getCharts().isEmpty()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No chart is defined");
            logger.log(Level.INFO, "Graph not added: No chart is defined");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            RequestContext.getCurrentInstance().update("formMsg");
            return;
        }

        checkGraphId();
        if (!validId) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Graph ID must be unique.");
            logger.log(Level.INFO, "Graph not added: Graph ID must be unique");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            RequestContext.getCurrentInstance().update("formMsg");
            return;
        }

        for (Chart c : graph.getCharts()) {
            if (c.getTypeInstance().contains(VAR_N)) {
                if (!graph.getVar().contains("n=")) {
                    FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Variable 'n' must be initialized, sicnce it is used by a chart.");
                    logger.log(Level.INFO, "Graph not added: Variable 'n' must be initialized, sicnce it is used by a chart.");
                    FacesContext.getCurrentInstance().addMessage(null, msg);
                    RequestContext.getCurrentInstance().update("formMsg");
//                    RequestContext.getCurrentInstance().execute("var.focus()");
                    return;
                }
            }
        }

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
        graph.setSelected(true);
        graphEjb.persistGraph(graph);

        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Graph added successfully.");
        logger.log(Level.INFO, "Graph added successfully");
        FacesContext.getCurrentInstance().addMessage(null, msg);
        RequestContext.getCurrentInstance().update("formMsg");
        RequestContext.getCurrentInstance().update("formMain");
        RequestContext.getCurrentInstance().execute("dlgNewGraph.hide()");
    }

    public void addChart() {
        chart.setPlugin(graph.getPlugin());
        chart.setPluginInstance(graph.getPluginInstance());
        graph.addChart(chart);
        RequestContext.getCurrentInstance().execute("dlgNewChart.hide()");
    }

    public void loadGroupsAndPlugins() {
        plugins = new ArrayList<String>();
        groups = graphEjb.findGroups(graph.getTarget());
        if (graph.ifTargetsService(ServiceType.KTHFS.toString())
                || graph.ifTargetsService(ServiceType.YARN.toString())
                || graph.ifTargetsService(ServiceType.Spark.toString())
                || graph.ifTargetsService(ServiceType.MapReduce.toString())) {
            plugins.add(CollectdPluginType.GenericJMX.toString());

        } else if (graph.ifTargetsRole(ServiceType.MySQLCluster.toString(), RoleType.ndb.toString())) {
            plugins.add(CollectdPluginType.dbi.toString());
        } else if (graph.ifTargets("HOST")) {
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
        graph.clearCharts();
        pluginInstances = new ArrayList<String>();
        if (!plugins.isEmpty() && isDbi()
                && graph.ifTargetsRole(ServiceType.MySQLCluster.toString(), RoleType.ndb.toString())) {
            for (String instance : dbiPluginInstances.keySet()) {
                pluginInstances.add(instance);
            }
        } else if (!plugins.isEmpty() && isGenericJMX()) {
            pluginInstanceTargetMap = new HashMap<String, String>();
            for (String target : jmxPluginInstances.keySet()) {
                if (graph.ifTargets(target)) {
                    for (String instance : jmxPluginInstances.get(target).keySet()) {
                        pluginInstances.add(instance);
                        pluginInstanceTargetMap.put(instance, target);
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

    public void pluginInstanceChanged() {
        graph.clearCharts();
    }

    private List<String> loadBasicTypes() {
        List<String> basicTypes = new ArrayList<String>();
        for (CollectdBasicType t : CollectdBasicType.values()) {
            basicTypes.add(t.toString());
        }
        return basicTypes;
    }

    public List<String> getTypes() {

        if (graph == null || graph.getTarget() == null || graph.getPlugin() == null
                || graph.getPluginInstance() == null || plugins.isEmpty()) {
            return null;
        }
        if (isDbi()) {
//            TODO : remove ?
            if (dbiPluginInstances == null) {
                dbiPluginInstances = CollectdConfigUtils.parseDbiPlugin(COLLECTD_DBI_PLUGIN_FILE);
            }
            types = dbiPluginInstances.get(graph.getPluginInstance()).getTypes();
        } else if (isGenericJMX()) {
//            TODO : remove ?
            if (jmxPluginInstances == null) {
                jmxPluginInstances = CollectdConfigUtils.parseJMXPlugin(COLLECTD_JMX_PLUGIN_FILE);
            }
            types = jmxPluginInstances.get(pluginInstanceTargetMap.get(graph.getPluginInstance())).get(graph.getPluginInstance()).getTypes();

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
        if (!plugins.isEmpty() && isGenericJMX()) {
            for (String instance : jmxPluginInstances.get(pluginInstanceTargetMap.get(graph.getPluginInstance()))
                    .get(graph.getPluginInstance()).getTypeInstances(chart.getType())) {
                typeInstances.add(instance);
            }
        }
        if (isDbi() || isGenericJMX()) {
            chart.setDs("value");
        }
        if (!typeInstances.isEmpty()) {
            chart.setTypeInstance(typeInstances.get(0));
            if (isTypeInstanceVariable()) {
                chart.setColor(ColorUtils.VAR_COLOR);
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
        return chart.getTypeInstance().contains(VAR_N);
    }
}
