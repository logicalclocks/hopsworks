package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.role.RoleType;
import se.kth.kthfsdashboard.service.ServiceType;
import se.kth.kthfsdashboard.struct.DatePeriod;
import se.kth.kthfsdashboard.util.UrlTools;

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
   @ManagedProperty("#{param.hostid}")
   private String hostId;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.role}")
   private String role;
   private Date start;
   private Date end;
   private String period;
   private List<DatePeriod> datePeriods = new ArrayList<DatePeriod>();
   private List<Integer> columns;
   private List<String> hostGraphs;
   private List<String> namenodeServiceGraphs;
   private List<String> namenodeGraphs;
   private List<String> namenodeServiceActivitiesGraphs;
   private List<String> namenodeActivitiesGraphs;
   private List<String> datanodeActivitiesGraphs;
   private List<String> mysqlclusterActivitiesGraphs;
   private static String URL_PATH = "/rest/collectd/graph?";
   private static final Logger logger = Logger.getLogger(GraphController.class.getName());

   public GraphController() {

      namenodeServiceGraphs = new ArrayList<String>(Arrays.asList(
              "serv_nn_capacity", "serv_nn_files", "serv_nn_load",
              "serv_nn_heartbeats", "serv_nn_blockreplication",
              "serv_nn_blocks", "serv_nn_specialblocks", "serv_nn_datanodes"));

      namenodeServiceActivitiesGraphs = new ArrayList<String>(Arrays.asList(
              "serv_nn_r_fileinfo", "serv_nn_r_getblocklocations", "serv_nn_r_getlisting",
              "serv_nn_r_getlinktarget", "serv_nn_r_filesingetlisting",
              "serv_nn_w_createfile_all", "serv_nn_w_filesappended", "serv_nn_w_filesrenamed",
              "serv_nn_w_deletefile_all", "serv_nn_w_addblock", "serv_nn_w_createsymlink",
              "serv_nn_o_getadditionaldatanode", "serv_nn_o_transactions", "serv_nn_o_transactionsbatchedinsync",
              "serv_nn_o_blockreport", "serv_nn_o_syncs",
              "serv_nn_t_safemodetime", "serv_nn_t_transactionsavgtime", "serv_nn_t_syncsavgtime",
              "serv_nn_t_blockreportavgtime"));


      namenodeGraphs = new ArrayList<String>(Arrays.asList("nn_capacity", "nn_files", "nn_load",
              "nn_heartbeats", "nn_blockreplication", "nn_blocks", "nn_specialblocks", "nn_datanodes"));

      namenodeActivitiesGraphs = new ArrayList<String>(Arrays.asList("nn_r_fileinfo", "nn_r_getblocklocations",
              "nn_r_getlisting", "nn_r_getlinktarget", "nn_r_filesingetlisting", "nn_w_createfile_all",
              "nn_w_filesappended", "nn_w_filesrenamed", "nn_w_deletefile_all", "nn_w_addblock", "nn_w_createsymlink",
              "nn_o_getadditionaldatanode", "nn_o_transactions", "nn_o_transactionsbatchedinsync", "nn_o_blockreport",
              "nn_o_syncs", "nn_t_safemodetime", "nn_t_transactionsavgtime",
              "nn_t_syncsavgtime", "nn_t_blockreportavgtime"));

      datanodeActivitiesGraphs = new ArrayList<String>(Arrays.asList("dd_heartbeats", "dd_avgTimeHeartbeats",
              "dd_bytes", "dd_opsReads", "dd_opsWrites", "dd_blocksRead", "dd_blocksWritten",
              "dd_blocksRemoved", "dd_blocksReplicated", "dd_blocksVerified",
              "dd_opsReadBlock", "dd_opsWriteBlock", "dd_opsCopyBlock", "dd_opsReplaceBlock",
              "dd_avgTimeReadBlock", "dd_avgTimeWriteBlock", "dd_avgTimeCopyBlock", "dd_avgTimeReplaceBlock",
              "dd_opsBlockChecksum", "dd_opsBlockReports", "dd_avgTimeBlockChecksum", "dd_avgTimeBlockReports",
              "dd_blockVerificationFailures", "dd_volumeFailures"));

      mysqlclusterActivitiesGraphs = new ArrayList<String>(Arrays.asList("mysql_freeDataMemory", "mysql_totalDataMemory",
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
   
   @PostConstruct
   public void init() {
      logger.info("init GraphController");
   }

   public String getCluster() {
      return cluster;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getHostId() {
      return hostId;
   }

   public void setHostId(String hostId) {
      this.hostId = hostId;
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

   public List<String> getNamenodeGraphs() {
      return namenodeGraphs;
   }

   public List<String> getNamenodeActivitiesGraphs() {
      return namenodeActivitiesGraphs;
   }

   public List<String> getDatanodeActivitiesGraphs() {
      return datanodeActivitiesGraphs;
   }

   public List<String> getMysqlclusterActivitiesGraphs() {
      return mysqlclusterActivitiesGraphs;
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

   public String graphUrl(String host, String plugin, String type, String chartType) throws MalformedURLException {
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("chart_type", chartType);
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      params.put("host", host);
      params.put("plugin", plugin);
      params.put("type", type);
      return UrlTools.addParams(URL_PATH, params);
   }

   public String hostGraphUrl(String plugin) throws MalformedURLException {
      String type = plugin.equals("interface") ? "if_octets" : plugin;
      return graphUrl(hostId, plugin, type, plugin + "all");
   }

   public String namenodesGraphUrl(String chartType, String cluster) {
      HashMap<String, String> params = new HashMap<String, String>();
      if (chartType.startsWith("serv_nn_")) {
         List<String> namenodes = roleEjb.findHostId(cluster,
                 ServiceType.KTHFS.toString(), RoleType.namenode.toString());
         String namenodesString = "";
         for (String namenode : namenodes) {
            namenodesString += namenodesString.isEmpty() ? namenode : "," + namenode;
         }
         params.put("host", namenodesString);
      }
      params.put("chart_type", chartType);
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      return UrlTools.addParams(URL_PATH, params);
   }

   public String namenodeGraphUrl(String chartType) {
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("chart_type", chartType);
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      params.put("host", hostId);
      return UrlTools.addParams(URL_PATH, params);
   }

   public String datanodeGraphUrl(String chartType) {
      return namenodeGraphUrl(chartType);
   }

   public String mysqlclusterGraphUrl(String chartType) {
      // Finds host of mysqld
      // Role=mysqld , Service=MySQLCluster, Clusters=cluster
      List<Role> roles = roleEjb.findRoles(cluster, service, "mysqld");
      String host = roles.size() > 0 ? roles.get(0).getHostId() : "";
      HashMap<String, String> params = new HashMap<String, String>();
      params.put("chart_type", chartType);
      params.put("plugin", "dbi-ndbinfo");      
      params.put("start", getStartTime().toString());
      params.put("end", getEndTime().toString());
      params.put("host", host);
      if (chartType.equals("mysql_freeDataMemory")
              || chartType.equals("mysql_freeIndexMemory")
              || chartType.equals("mysql_totalDataMemory")
              || chartType.equals("mysql_totalIndexMemory")) {
         Long n = roleEjb.count(cluster, service, "ndb");
         params.put("n", n.toString());
      }
      return UrlTools.addParams(URL_PATH, params);
   }
   
   public boolean showNamenodeGraphs() {
      if (role.equals("namenode")) {
         return true;
      }
      return false;
   }

   public boolean roleHasGraphs() {
      if (role == null) {
         return false;
      }
      if (role.equals("datanode") || role.equals("namenode")) {
         return true;
      }
      return false;
   }

   public boolean showDatanodeGraphs() {
      if (role.equals("datanode")) {
         return true;
      }
      return false;
   }   
}