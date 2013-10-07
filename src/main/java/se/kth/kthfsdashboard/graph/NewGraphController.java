package se.kth.kthfsdashboard.graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
import se.kth.kthfsdashboard.struct.CollectdPluginInstance;
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
    private static final List<String> targets;
    private static final List<ChartModel> models;
    private static final List<String> colors;
    private static final List<String> formats;
    private static List<String> types;
    private static List<String> typeInstances;
    private List<String> groups;
    private HashMap<String, List<String>> pluginsMap;
    private List<String> plugins;
    private List<String> pluginInstances;
    private boolean validId;
    private Graph graph;
    private List<Chart> charts;
    private Chart chart = new Chart();
    private String typeInstanceInfo;
    private static final String COLLECTD_CONFIG_FILE = "/etc/collectd/collectd.conf";
    private static final String COLLECTD_DBI_CONFIG_FILE = "/home/x/NetBeansProjects/kthfs-pantry/cookbooks/collect/templates/default/dbi_plugin.conf.erb";
    private static final Logger logger = Logger.getLogger(NewGraphController.class.getName());
    private static Map<String, CollectdPluginInstance> dbiPluginInstances;
    @ManagedProperty(value = "#{EditGraphController}")
    private EditGraphsController editGraphController;
    private static final String COLOR_N = "COLOR(@n)";
    
    static {
        targets = new ArrayList<String>();
        targets.add("HOST");
        targets.add("KTHFS");
        targets.add("KTHFS-DATANODE");
        targets.add("KTHFS-NAMENODE");
        targets.add("MYSQLCLUSTER");
        targets.add("MYSQLCLUSTER-NDB");
        targets.add("MYSQLCLUSTER-MGMSERVER");
        targets.add("MYSQLCLUSTER-MYSQLD");
        targets.add("MYSQLCLUSTER-MEMCACHED");
        targets.add("YARN");
        targets.add("YARN-NODEMANAGER");
        targets.add("YARN-RESOURCEMANAGER");
        
        models = new ArrayList<ChartModel>();
        models.add(ChartModel.LINE);
        models.add(ChartModel.LINES);
        models.add(ChartModel.SUM_LINE);
        models.add(ChartModel.AVG_LINE);
        models.add(ChartModel.AREA);
        models.add(ChartModel.AREA_STACK);
        
        colors = new ArrayList<String>();
        colors.add("GREEN");
        colors.add("RED");
        colors.add("BLUE");
        colors.add("YELLOW");
        colors.add(COLOR_N);
        
        formats = new ArrayList<String>();
        formats.add("%5.2lf");
        formats.add("%5.2lf %S");
    }
    
    public NewGraphController() {
    }
    
    @PostConstruct
    public void init() {
        logger.info("init NewGraphsController");
        
        pluginsMap = new HashMap<String, List<String>>();
        pluginsMap.put("load", null);
        pluginsMap.put("memory", null);
        pluginsMap.put("dbi", Arrays.asList("ndbinfo"));
        pluginsMap.put("GenericJMX", Arrays.asList("FSNamesystem", "FSNamesystemState",
                "NameNodeActivity", "DataNodeActivity", "NodeManager", "ResourceManager"));
        
        dbiPluginInstances = CollectdConfigUtils.parseDbiconfig(COLLECTD_DBI_CONFIG_FILE);
    }
    
    public List<String> getTargets() {
        return targets;
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
    
    public List<ChartModel> getModels() {
        return models;
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
    
    public List<String> getColors() {
        return colors;
    }
    
    public List<String> getFormats() {
        return formats;
    }
    
    public List<String> getTypeInstances() {
        return typeInstances;
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
        
        System.out.println("Graph added");
    }
    
    public void addChart() {
        chart.setPlugin(graph.getPlugin());
        chart.setPluginInstance(graph.getPluginInstance());
        charts.add(chart);
        RequestContext.getCurrentInstance().execute("dlgNewChart.hide()");
    }
    
    public void showNewGraphDialog() {
        graph = new Graph();
        graph.setGraphId("testId");
        charts = new ArrayList<Chart>();
        groups = null;
        plugins = null;
        pluginInstances = null;
        // TODO This can be removed. It is also loaded in init
        dbiPluginInstances = CollectdConfigUtils.parseDbiconfig(COLLECTD_DBI_CONFIG_FILE);
        
        RequestContext.getCurrentInstance().update("formNew");
        RequestContext.getCurrentInstance().reset("formNew");
        RequestContext.getCurrentInstance().execute("dlgNewGraph.show()");
    }
    
    public void showNewChartDialog() {
        chart = new Chart();
        RequestContext.getCurrentInstance().update("formNewChart");
        RequestContext.getCurrentInstance().execute("dlgNewChart.show()");
    }
    
    public void loadGroupsAndPlugins() {
        
        groups = graphEjb.findGroups(graph.getTarget());
        if (!groups.isEmpty()) {
            graph.setGroup(groups.get(0));
        } else {
            graph.setGroup(null);
        }
        plugins = new ArrayList<String>();
        if (graph.getTarget().startsWith("KTHFS")) {
            plugins.add("GenericJMX");
        } else if (graph.getTarget().startsWith("YARN")) {
            plugins.add("GenericJMX");
        } else if (graph.getTarget().startsWith("MYSQLCLUSTER")) {
            plugins.add("dbi");
        } else {
            loadPluginsFromConfFile();
        }
        if (!plugins.isEmpty()) {
            graph.setPlugin(plugins.get(0));
        } else {
            graph.setPlugin(null);
        }
        loadPluginInstance();
    }
    
    public void loadPluginInstance() {
        if (!plugins.isEmpty() && isDbi()) {
            pluginInstances = new ArrayList<String>();
            for (String instance : dbiPluginInstances.keySet()) {
                pluginInstances.add(instance);
            }
        } else {
            pluginInstances = pluginsMap.get(graph.getPlugin());
        }
        
        if (pluginInstances != null && !pluginInstances.isEmpty()) {
            graph.setPluginInstance(pluginInstances.get(0));
        } else {
            graph.setPluginInstance(null);
        }
    }
    
    public List<String> getTypes() {
        
        if (graph == null || graph.getTarget() == null || graph.getPluginInstance() == null) {
            return null;
        }
        
        if (!plugins.isEmpty() && isDbi()) {
            if (dbiPluginInstances == null) {
                dbiPluginInstances = CollectdConfigUtils.parseDbiconfig(COLLECTD_DBI_CONFIG_FILE);
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
        System.out.println("***********");
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
                chart.setColor(colors.get(0));
                chart.setModel(ChartModel.LINE);
            }
        } else {
            chart.setType(null);
            chart.setModel(ChartModel.LINE);
        }
    }
    
    public void changedPluginInstance() {
        for (Chart c : charts) {
            c.setPluginInstance(graph.getPluginInstance());
        }
    }
    
    private List<String> loadBasicTypes() {
        List<String> basicTypes = new ArrayList<String>();
        basicTypes.add("guage");
        basicTypes.add("counter");
        basicTypes.add("derive");
        basicTypes.add("absolute");
        return basicTypes;
    }
    
    private void loadPluginsFromConfFile() {
        plugins = new ArrayList<String>();
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(COLLECTD_CONFIG_FILE));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("LoadPlugin")
                        && !line.contains("rrdtool")
                        && !line.contains("logfile")
                        && !line.contains("syslog")
                        && !line.contains("network")) {
                    plugins.add(line.trim().replaceAll(" +", " ").split(" ")[1]);
                }
            }
        } catch (IOException ex) {
            plugins.clear();
            logger.log(Level.SEVERE, ex.toString());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.toString());
            }
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
        return graph.getPlugin().equals("dbi");
    }
    
    private boolean isTypeInstanceVariable() {
        return chart.getTypeInstance().contains("@n");
        
    }
}