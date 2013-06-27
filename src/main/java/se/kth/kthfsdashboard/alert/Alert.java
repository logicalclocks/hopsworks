package se.kth.kthfsdashboard.alert;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import se.kth.kthfsdashboard.util.Formatter;


/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "Alerts")
@NamedQueries({
   @NamedQuery(name = "Alerts.findAll", query = "SELECT a FROM Alert a ORDER BY a.alertTime DESC"),
   @NamedQuery(name = "Alerts.removeAll", query = "DELETE FROM Alert a")   
})
public class Alert implements Serializable {

   public enum Severity {
      FAILURE, WARNING, OKAY
   }
   
   @Id
   @GeneratedValue(strategy = GenerationType.SEQUENCE)
   private Long id;
   @Column(name = "message", nullable = false, length = 512)
   private String message;
   @Column(name = "host_name", length = 128)
   private String hostId;
   @Temporal(javax.persistence.TemporalType.TIMESTAMP)
   private Date alertTime;
   private long agentTime;
   @Column(name = "plugin", length = 32)
   private String plugin;
   @Column(name = "plugin_instance", length = 32)
   private String pluginInstance;
   @Column(name = "type", length = 32)
   private String type;
   @Column(name = "type_instance", length = 32)
   private String typeInstance;

   @Column(name = "data_source", length = 32)
   private String dataSource;   
   
   @Column(length = 16)
   private String CurrentValue;
   @Column(length = 16)
   private String WarningMin;
   @Column(length = 16)
   private String WarningMax;   
   @Column(length = 16)
   private String FailureMin;
   @Column(length = 16)
   private String FailureMax; 
   
   private Severity severity;

   public Alert() {
   }

   public Alert(String hostId, String message, String plugin, String pluginInstance, String type, String typeInstance) {
      this.hostId = hostId;
      this.message = message;
      this.plugin = plugin;
      this.pluginInstance = pluginInstance;
      this.type = type;
      this.typeInstance = typeInstance;
   }

   public Long getId() {
      return id;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public String getHostId() {
      return hostId;
   }

   public void setHostId(String hostId) {
      this.hostId = hostId;
   }

   public Date getAlertTime() {
      return alertTime;
   }

   public void setAlertTime(Date alertTime) {
      this.alertTime = alertTime;
   }

   public long getAgentTime() {
      return agentTime;
   }

   public void setAgentTime(long agentTime) {
      this.agentTime = agentTime;
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

   public String getDataSource() {
      return dataSource;
   }

   public void setDataSource(String dataSource) {
      this.dataSource = dataSource;
   }

   public String getCurrentValue() {
      return CurrentValue;
   }

   public void setCurrentValue(String CurrentValue) {
      this.CurrentValue = CurrentValue;
   }

   public String getWarningMin() {
      return WarningMin;
   }

   public void setWarningMin(String WarningMin) {
      this.WarningMin = WarningMin;
   }

   public String getWarningMax() {
      return WarningMax;
   }

   public void setWarningMax(String WarningMax) {
      this.WarningMax = WarningMax;
   }

   public String getFailureMin() {
      return FailureMin;
   }

   public void setFailureMin(String FailureMin) {
      this.FailureMin = FailureMin;
   }

   public String getFailureMax() {
      return FailureMax;
   }

   public void setFailureMax(String FailureMax) {
      this.FailureMax = FailureMax;
   }

   public Severity getSeverity() {
      return severity;
   }

   public void setSeverity(Severity severity) {
      this.severity = severity;
   }
   
   public String getAlertTimeShort() {
      return Formatter.date(alertTime);
   }


}