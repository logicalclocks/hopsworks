package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.Role.RoleType;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.service.ServiceType;
import se.kth.kthfsdashboard.struct.DatePeriod;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class GraphController implements Serializable {

   @EJB
   private RoleEJB roleEjb;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   @ManagedProperty("#{param.hostname}")
   private String hostname;
   @ManagedProperty("#{param.service}")
   private String service;
   private String role;
   private Date start;
   private Date end;
   private String period;
   private List<DatePeriod> datePeriods = new ArrayList<DatePeriod>();
   private List<Integer> columns;
   private List<String> hostGraphs;
   private List<String> namenodeServiceGraphs;
   private List<String> namenodeInstanceGraphs;
   private List<String> namenodeServiceActivitiesGraphs;
   private List<String> namenodeInstanceActivitiesGraphs;
   private List<String> datanodeInstanceActivitiesGraphs;
   private List<String> mysqlclusterInstanceActivitiesGraphs;

   public GraphController() {

      namenodeServiceGraphs = new ArrayList<String>(Arrays.asList("serv_nn_capacity", "serv_nn_files",
              "serv_nn_load", "serv_nn_heartbeats", "serv_nn_blockreplication",
              "serv_nn_blocks", "serv_nn_specialblocks", "serv_nn_datanodes"));

      namenodeServiceActivitiesGraphs = new ArrayList<String>(Arrays.asList("serv_nn_r_fileinfo", "serv_nn_r_getblocklocations",
              "serv_nn_r_getlisting", "serv_nn_r_getlinktarget", "serv_nn_r_filesingetlisting",
              "serv_nn_w_createfile_all", "serv_nn_w_filesappended", "serv_nn_w_filesrenamed",
              "serv_nn_w_deletefile_all", "serv_nn_w_addblock", "serv_nn_w_createsymlink",
              "serv_nn_o_getadditionaldatanode", "serv_nn_o_transactions", "serv_nn_o_transactionsbatchedinsync",
              "serv_nn_o_blockreport", "serv_nn_o_syncs",
              "serv_nn_t_safemodetime", "serv_nn_t_transactionsavgtime", "serv_nn_t_syncsavgtime",
              "serv_nn_t_blockreportavgtime"));


      namenodeInstanceGraphs = new ArrayList<String>(Arrays.asList("nn_capacity", "nn_files", "nn_load",
              "nn_heartbeats", "nn_blockreplication", "nn_blocks", "nn_specialblocks", "nn_datanodes"));

      namenodeInstanceActivitiesGraphs = new ArrayList<String>(Arrays.asList("nn_r_fileinfo", "nn_r_getblocklocations",
              "nn_r_getlisting", "nn_r_getlinktarget", "nn_r_filesingetlisting", "nn_w_createfile_all",
              "nn_w_filesappended", "nn_w_filesrenamed", "nn_w_deletefile_all", "nn_w_addblock", "nn_w_createsymlink",
              "nn_o_getadditionaldatanode", "nn_o_transactions", "nn_o_transactionsbatchedinsync", "nn_o_blockreport",
              "nn_o_syncs", "nn_t_safemodetime", "nn_t_transactionsavgtime",
              "nn_t_syncsavgtime", "nn_t_blockreportavgtime"));

      datanodeInstanceActivitiesGraphs = new ArrayList<String>(Arrays.asList("dd_heartbeats", "dd_avgTimeHeartbeats",
              "dd_bytes", "dd_opsReads", "dd_opsWrites", "dd_blocksRead", "dd_blocksWritten",
              "dd_blocksRemoved", "dd_blocksReplicated", "dd_blocksVerified",
              "dd_opsReadBlock", "dd_opsWriteBlock", "dd_opsCopyBlock", "dd_opsReplaceBlock",
              "dd_avgTimeReadBlock", "dd_avgTimeWriteBlock", "dd_avgTimeCopyBlock", "dd_avgTimeReplaceBlock",
              "dd_opsBlockChecksum", "dd_opsBlockReports", "dd_avgTimeBlockChecksum", "dd_avgTimeBlockReports",
              "dd_blockVerificationFailures", "dd_volumeFailures"));

      mysqlclusterInstanceActivitiesGraphs = new ArrayList<String>(Arrays.asList("mysql_freeDataMemory", "mysql_totalDataMemory",
              "mysql_freeIndexMemory", "mysql_totalIndexMemory", "mysql_simpleReads", "mysql_Reads", "mysql_Writes",
              "mysql_rangeScans", "mysql_tableScans"));

      hostGraphs = new ArrayList<String>(Arrays.asList("load", "memory", "df", "interface", "swap"));

      columns = new ArrayList<Integer>(Arrays.asList(2, 3, 4, 5));

      datePeriods.add(new DatePeriod("hour", "1h"));
      datePeriods.add(new DatePeriod("2hr", "2h"));
      datePeriods.add(new DatePeriod("4hr", "4h"));
      datePeriods.add(new DatePeriod("day", "1d"));
      datePeriods.add(new DatePeriod("week", "7d"));
      datePeriods.add(new DatePeriod("month", "1m"));
      datePeriods.add(new DatePeriod("year", "1y"));

      Calendar c = Calendar.getInstance();
      c.setTime(new Date());
      c.add(Calendar.HOUR_OF_DAY, -1);
      start = c.getTime();
      end = new Date();

      period = "1h";
   }

   public String getCluster() {
      return cluster;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getHostname() {
      return hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public String getService() {
      return service;
   }

   public void setService(String service) {
      this.service = service;
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }

   public Date getStart() {
      return start;
   }

   public void setStart(Date start) {
      this.start = start;
   }

   public Long getStartTime() {
      return longTime(getStart());
   }

   public Date getEnd() {
      return end;
   }

   public void setEnd(Date end) {
      this.end = end;
   }

   public Long getEndTime() {
      return longTime(getEnd());
   }

   public void updateDates() {

      Calendar c = Calendar.getInstance();
      c.setTime(new Date());
      String unit = period.substring(period.length() - 1);
      int delta = Integer.parseInt(period.substring(0, period.length() - 1));

      if (unit.equals("h")) {
         c.add(Calendar.HOUR_OF_DAY, -delta);
      } else if (unit.equals("d")) {
         c.add(Calendar.DAY_OF_MONTH, -delta);
      } else if (unit.equals("m")) {
         c.add(Calendar.MONTH, -delta);
      } else if (unit.equals("y")) {
         c.add(Calendar.YEAR, -delta);
      } else {
         return;
      }
      start = c.getTime();
      end = new Date();
   }

   public void useCalendar() {
      period = null;
   }

   private Long longTime(Date d) {
      return d.getTime() / 1000;
   }

   public String getPeriod() {
      return period;
   }

   public void setPeriod(String period) {
      this.period = period;
   }

   public List<DatePeriod> getDatePeriods() {
      return datePeriods;
   }

   public List<String> getNamenodeServiceGraphs() {
      return namenodeServiceGraphs;
   }

   public String getPlaneGraphUrl() throws MalformedURLException {
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      params.put("hostname", "ubuntu");
      params.put("plugin", "memory");
//      params.put("plugin_instance", "memory");
      params.put("type", "memory");
      params.put("type_instance", "used");

      String url = "rest/collectd/graph?";
      for (Entry<String, String> entry : params.entrySet()) {
         url += entry.getKey() + "=" + entry.getValue() + "&";
      }
      return url;
   }

   public String getGraphUrl(String host, String plugin, String type, String chartType) throws MalformedURLException {
      String url = "../rest/collectd/graph?";
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("chart_type", chartType);
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      params.put("hostname", host);
      params.put("plugin", plugin);
      params.put("type", type);
      for (Entry<String, String> entry : params.entrySet()) {
         url += entry.getKey() + "=" + entry.getValue() + "&";
      }
      return url;
   }

   public String getHostGraphUrl(String plugin) throws MalformedURLException {

      String type;
      if (plugin.equals("interface")) {
         type = "if_octets";
      } else {
         type = plugin;
      }
      return getGraphUrl(hostname, plugin, type, plugin + "all");
   }

   public String getGraphUrl(String host, String plugin, String type) throws MalformedURLException {
//      TODO: host/hostname ?
      return getGraphUrl(hostname, plugin, type, plugin + "all");
   }

   public String getNamenodeGraphUrl(String role, String chartType) {
      String url = "../rest/collectd/graph?";
      HashMap<String, String> params = new HashMap<String, String>();

      if (chartType.startsWith("serv_nn_")) {
         List<String> namenodes = roleEjb.findHostname(cluster, ServiceType.KTHFS.toString(), RoleType.namenode.toString());
         String namenodesString = "";
         for (String namenode : namenodes) {
            if (!namenodesString.isEmpty()) {
               namenodesString += ",";
            }
            namenodesString += namenode;
         }
         params.put("hostname", namenodesString);
      }
      params.put("chart_type", chartType);
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      for (Entry<String, String> entry : params.entrySet()) {
         url += entry.getKey() + "=" + entry.getValue() + "&";
      }
      return url;
   }

   public String getNamenodeInstanceGraphUrl(String chartType) {
      String url = "../rest/collectd/graph?";
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("chart_type", chartType);
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      params.put("hostname", hostname);
      for (Entry<String, String> entry : params.entrySet()) {
         url += entry.getKey() + "=" + entry.getValue() + "&";
      }
      return url;
   }

   public String getDatanodeInstanceGraphUrl(String chartType) {
      return getNamenodeInstanceGraphUrl(chartType);
   }

   public String getMysqlclusterInstanceGraphUrl(String chartType) {

      // Finds hostname of mysqld
      // Role=mysqld , Service=MySQLCluster, Clusters=cluster
      final String MYSQLD_ROLE = "mysqld";

      List<Role> roles = roleEjb.findRoles(cluster, service, MYSQLD_ROLE);
      String host = "";
      if (roles.size() > 0) {
         host = roles.get(0).getHostname();
      }
      String url = "../rest/collectd/graph?";
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("chart_type", chartType);
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      params.put("hostname", host);
      if (chartType.equals("mysql_freeDataMemory") || chartType.equals("mysql_freeIndexMemory")
              || chartType.equals("mysql_totalDataMemory") || chartType.equals("mysql_totalIndexMemory")) {
         final String NDB_ROLE = "ndb";
         Long n = roleEjb.count(cluster, service, NDB_ROLE);
         params.put("n", n.toString());
      }
      for (Entry<String, String> entry : params.entrySet()) {
         url += entry.getKey() + "=" + entry.getValue() + "&";
      }
      return url;
   }

   public List<Integer> getColumns() {
      return columns;
   }

   public void setColumns(List<Integer> columns) {
      this.columns = columns;
   }

   public List<String> getNamenodeServiceActivitiesGraphs() {
      return namenodeServiceActivitiesGraphs;
   }

   public List<String> getHostGraphs() {
      return hostGraphs;
   }

   public List<String> getNamenodeInstanceGraphs() {
      return namenodeInstanceGraphs;
   }

   public List<String> getNamenodeInstanceActivitiesGraphs() {
      return namenodeInstanceActivitiesGraphs;
   }

   public List<String> getDatanodeInstanceActivitiesGraphs() {
      return datanodeInstanceActivitiesGraphs;
   }

   public List<String> getMysqlclusterInstanceActivitiesGraphs() {
      return mysqlclusterInstanceActivitiesGraphs;
   }
}