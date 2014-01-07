package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "Graphs")
@NamedQueries({
    @NamedQuery(name = "Graphs.find", query = "SELECT g FROM Graph g WHERE g.graphId = :graphId AND g.target = :target"),
    @NamedQuery(name = "Graphs.find-By.GraphId", query = "SELECT g FROM Graph g WHERE g.graphId = :graphId"),
    @NamedQuery(name = "Graphs.find-By.Target", query = "SELECT g FROM Graph g WHERE g.target = :target ORDER BY g.groupRank, g.rank"),
    @NamedQuery(name = "Graphs.find.Targets", query = "SELECT DISTINCT(g.target) FROM Graph g ORDER BY g.target"),
    @NamedQuery(name = "Graphs.find.SelectedIds-By.Target.Group", query = "SELECT g.graphId FROM Graph g WHERE g.selected = TRUE AND g.target = :target AND g.group = :group ORDER BY g.rank"),
    @NamedQuery(name = "Graphs.find.Ids-By.Target", query = "SELECT g.graphId FROM Graph g WHERE g.target = :target ORDER BY g.rank"),
    @NamedQuery(name = "Graphs.find.Groups-By.Target", query = "SELECT DISTINCT(g.group) FROM Graph g WHERE g.selected = TRUE AND g.target = :target ORDER BY g.groupRank"),
    @NamedQuery(name = "Graphs.find.LastGroupRank-By.Target", query = "SELECT MAX(g.groupRank) FROM Graph g WHERE g.target = :target"),
    @NamedQuery(name = "Graphs.find.GroupRank-By.Target.Group", query = "SELECT DISTINCT(g.groupRank) FROM Graph g WHERE g.target = :target AND g.group = :group"),
    @NamedQuery(name = "Graphs.find.lastRank-By.Target.Group", query = "SELECT MAX(g.rank) FROM Graph g WHERE g.target = :target AND g.group = :group"),
    @NamedQuery(name = "Graphs.removeAll", query = "DELETE FROM Graph g"),})
public class Graph implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(nullable = false, length = 64)
    private String graphId;
    @Column(nullable = false, length = 64)
    private String target;
    @Column(name = "GROUP_", nullable = false, length = 64)
    private String group;
    @Column(length = 48)
    private String plugin;
    @Column(length = 48)
    private String pluginInstance;
    private int rank;
    private int groupRank;
    @Column(length = 48)
    private String title;
    @Column(length = 64)
    private String var;
    @Column(length = 64)
    private String verticalLabel;
    private boolean selected;
    private List<Chart> charts = new ArrayList<Chart>();

    public Graph() {
        this.target = null;
        this.group = null;
        this.graphId = null;
        this.var = null;
        this.plugin = "";
        this.pluginInstance = null;
        this.title = null;
        this.verticalLabel = null;
    }

    public Graph(String target, String group, int groupRank, int rank, 
            String graphId, String var, String plugin, String pluginInstance, 
            String title, String verticalLabel) {
        this.target = target;
        this.group = group;
        this.graphId = graphId;
        this.var = var;
        this.plugin = (plugin == null) ? "" : plugin;
        this.pluginInstance = pluginInstance;
        this.groupRank = groupRank;
        this.rank = rank;
        this.title = title;
        this.verticalLabel = verticalLabel;
        this.selected = true;
    }

    public void addChart(Chart chart) {
        charts.add(chart);
    }

    @Override
    public String toString() {
        return "Graph {Target: " + target + ", Group: " + group + ", Id: " + graphId
                + ", Plugin: " + plugin + ", Plugin-Instance: " + pluginInstance + "}";
    }

    public String getChartSet() {
        Set<String> chartSet = new HashSet<String>();
        for (Chart chart : charts) {
            chartSet.add(chart.getRrdFileName());
        }
        return chartSet.toString();
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getId() {
        return id;
    }

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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

    public int getRank() {
        return rank;
    }

    public int getGroupRank() {
        return groupRank;
    }

    public void setGroupRank(int groupRank) {
        this.groupRank = groupRank;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVar() {
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public String getVerticalLabel() {
        return verticalLabel;
    }

    public void setVerticalLabel(String verticalLabel) {
        this.verticalLabel = verticalLabel;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<Chart> getCharts() {
        return charts;
    }
    
    public void clearCharts() {
        charts.clear();
    }

    public boolean ifTargetsService(String service) {
        return ifTargets(service.toUpperCase());
    }

    public boolean ifTargetsRole(String service, String role) {
        return ifTargets(service.toUpperCase() + "-" + role.toUpperCase());
    }

    public boolean ifTargets(String target) {
        return this.target.startsWith(target.toUpperCase())
                || target.toUpperCase().startsWith(this.target);
    }
}
