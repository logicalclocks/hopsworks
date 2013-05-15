package se.kth.kthfsdashboard.log;

import java.io.Serializable;
import javax.persistence.*;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "Logs")
@NamedQueries({
    @NamedQuery(name = "findAll", query = "SELECT m FROM Log m"),
    @NamedQuery(name = "findLatestLogTime", query = "SELECT MAX(l.time) FROM Log l WHERE l.host = :host"),
    @NamedQuery(name = "findLatestLogForPlugin", query = "SELECT l FROM Log l WHERE l.host = :host AND l.plugin = :plugin AND ((SELECT MAX(h.time) FROM Log h WHERE h.host = :host AND h.plugin = :plugin) = l.time)"),
    @NamedQuery(name = "findLatestLogForPluginAndType", query = "SELECT l FROM Log l WHERE l.host = :host AND l.plugin = :plugin AND l.type = :type AND ((SELECT MAX(h.time) FROM Log h WHERE h.host = :host AND h.plugin = :plugin AND h.type = :type) = l.time)"),
    @NamedQuery(name = "findLogForPluginAndTypeAtTime", query = "SELECT l FROM Log l WHERE l.host = :host AND l.plugin = :plugin AND l.type = :type AND l.time = :time"),
    @NamedQuery(name = "findNumberOfCpu", query = "SELECT COUNT(DISTINCT l.pluginInstance) FROM Log l WHERE l.host = :host AND l.plugin = 'cpu'"),
//    @NamedQuery(name = "findLastestLog", query = "SELECT l.id, l.host, l.interval, l.plugin, l.pluginInstance, MAX(l.time) as time, l.type, l.typeInstance, l.values FROM Log l WHERE l.host = :host AND l.type = :type AND l.plugin = :plugin GROUP BY l.typeInstance")
    @NamedQuery(name = "findLastestLog", query = "SELECT NEW se.kth.kthfsdashboard.log.Log(l.id, l.values, MAX(l.time), l.interval, l.host, l.plugin, l.pluginInstance, l.type, l.typeInstance) FROM Log l WHERE l.host = :host AND l.type = :type AND l.plugin = :plugin GROUP BY l.typeInstance")
})
//@NamedNativeQueries({
//    @NamedNativeQuery(name = "findLastestLog", query = "SELECT l.id, l.values, MAX(l.time), l.interval, l.host, l.plugin, l.pluginInstance, l.type, l.typeInstance FROM Log l WHERE l.host = :host AND l.type = :type AND l.plugin = :plugin GROUP BY l.typeInstance", resultClass = Log.class)
//})
public class Log implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(name = "values_", nullable = false, length = 48)
    private String values;
    @Column(name = "time_", nullable = false)
    private long time;
    @Column(name = "interval_", columnDefinition = "TINYINT")
    private int interval;
    @Column(name = "host_", nullable = false, length = 48)
    private String host;
    @Column(name = "plugin", nullable = false, length = 16)
    private String plugin;
    @Column(name = "plugin_instance", length = 16)
    private String pluginInstance;
    @Column(name = "type", nullable = false, length = 16)
    private String type;
    @Column(name = "type_instance", length = 16)
    private String typeInstance;

    public Log() {
    }

    //with id
    public Log(Long id, String values, Object time, int interval, String host,
            String plugin, String pluginInstance, String type, String typeInstance) {
        this.id = id;
        this.values = values;
        this.time = (Long)time;
        this.interval = interval;
        this.host = host;
        this.plugin = plugin;
        this.pluginInstance = pluginInstance;
        this.type = type;
        this.typeInstance = typeInstance;
    }

    public Log(String values, long time, int interval, String host,
            String plugin, String pluginInstance, String type, String typeInstance) {
        this.values = values;
        this.time = time;
        this.interval = interval;
        this.host = host;
        this.plugin = plugin;
        this.pluginInstance = pluginInstance;
        this.type = type;
        this.typeInstance = typeInstance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValues() {
        return values;
    }

    public void setValues(String values) {
        this.values = values;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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
}
