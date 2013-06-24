package se.kth.kthfsdashboard.service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.Role;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.role.RoleType;
import se.kth.kthfsdashboard.role.Status;
import se.kth.kthfsdashboard.struct.ServiceRoleInfo;
import se.kth.kthfsdashboard.util.WebCommunication;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class ServiceController {

   @EJB
   private HostEJB hostEJB;
   @EJB
   private RoleEJB roleEjb;
   @ManagedProperty("#{param.hostname}")
   private String hostname;
   @ManagedProperty("#{param.role}")
   private String role;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   @ManagedProperty("#{param.status}")
   private String status;
   public static String NOT_AVAILABLE = "Not available.";
   public static Map<String, List<ServiceRoleInfo>> rolesMap = new HashMap<String, List<ServiceRoleInfo>>();
   public static Map<String, String> servicesRolesMap = new HashMap<String, String>();
   
   private static final Logger logger = Logger.getLogger(ServiceController.class.getName());   

   public ServiceController() {

      List<ServiceRoleInfo> roles;

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("NameNode", RoleType.namenode.toString()));
      roles.add(new ServiceRoleInfo("DataNode", RoleType.datanode.toString()));
      rolesMap.put(ServiceType.KTHFS.toString(), roles);

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("MySQL Cluster NDBD (ndb)", RoleType.ndb.toString()));
      roles.add(new ServiceRoleInfo("MySQL Server (mysqld)", RoleType.mysqld.toString()));
      roles.add(new ServiceRoleInfo("MGM Server (mgmserver)", RoleType.mgmserver.toString()));
      rolesMap.put(ServiceType.MySQLCluster.toString(), roles);

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("Resource Manager", RoleType.resourcemanager.toString()));
      roles.add(new ServiceRoleInfo("Node Manager", RoleType.nodemanager.toString()));
      rolesMap.put(ServiceType.YARN.toString(), roles);

      servicesRolesMap.put(RoleType.namenode.toString(), ServiceType.KTHFS.toString());
      servicesRolesMap.put(RoleType.datanode.toString(), ServiceType.KTHFS.toString());

      servicesRolesMap.put(RoleType.ndb.toString(), ServiceType.MySQLCluster.toString());
      servicesRolesMap.put(RoleType.mysqld.toString(), ServiceType.MySQLCluster.toString());
      servicesRolesMap.put(RoleType.mgmserver.toString(), ServiceType.MySQLCluster.toString());

      servicesRolesMap.put(RoleType.resourcemanager.toString(), ServiceType.YARN.toString());
      servicesRolesMap.put(RoleType.nodemanager.toString(), ServiceType.YARN.toString());
   }
   
   @PostConstruct
   public void init() {
      logger.info("init ServiceController");
   }      

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }

   public String getService() {
      return service;
   }

   public void setService(String service) {
      this.service = service;
   }

   public String getHostname() {
      return hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getCluster() {
      return cluster;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getStatus() {
      return status;
   }

   public List<ServiceRoleInfo> getRoles() {

      List<ServiceRoleInfo> serviceRoles = new ArrayList<ServiceRoleInfo>();
      for (ServiceRoleInfo role : rolesMap.get(service)) {
         serviceRoles.add(setStatus(cluster, service, role));
      }
      return serviceRoles;
   }

   private ServiceRoleInfo setStatus(String cluster, String group, ServiceRoleInfo role) {
      int started, stopped, failed, good, bad;
      started = roleEjb.countStatus(cluster, group, role.getShortName(), Status.Started);
      stopped = roleEjb.countStatus(cluster, group, role.getShortName(), Status.Stopped);
      failed = roleEjb.countStatus(cluster, group, role.getShortName(), Status.Failed);
      good = started;
      bad = failed + stopped;
      role.setStatusStarted(started + " Started");
      role.setStatusStopped((stopped + failed) + " Stopped");
      role.setHealth(String.format("%d Good, %d Bad", good, bad));
      return role;
   }

   public void addMessage(String summary) {
      FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
      FacesContext.getCurrentInstance().addMessage(null, message);
   }

   public void startService() {
      addMessage("Start not implemented!");
   }

   public void stopService() {
      addMessage("Stop not implemented!");
   }

   public void restartService() {
      addMessage("Restart not implemented!");
   }

   public void deleteService() {
      addMessage("Delete not implemented!");
   }

   public boolean hasWebUi() {
      try {
      Role r = roleEjb.find(hostname, cluster, service, role);
      if (r == null || r.getWebPort() == null || r.getWebPort() == 0) {
         return false;
      }
      return true;
      } catch(Exception ex) {
         return false;
      }
   }

   public String getRoleLog(int lines) {
      try {
         WebCommunication webComm = new WebCommunication();
         String ip = findIpByHostname(hostname);
         return webComm.getRoleLog(ip, cluster, service, role, lines);
      } catch (Exception ex) {
         return ex.getMessage();
      }
   }

   public String getServiceLog(int lines) {
      try {
         String ip = findIpByRole(cluster, service, "mgmserver");
         WebCommunication webComm = new WebCommunication();
         return webComm.getServiceLog(ip, cluster, service, lines);
      } catch (Exception ex) {
         return ex.getMessage();
      }
   }

   public String getAgentLog(int lines) {
      try {
         WebCommunication webCom = new WebCommunication();
         String ip = findIpByHostname(hostname);
         return webCom.getAgentLog(ip, lines);
      } catch (Exception ex) {
         return ex.getMessage();
      }
   }

   public String getMySQLClusterConfig() throws Exception {

      // Finds hostname of mgmserver
      // Role=mgmserver , Service=MySQLCluster, Cluster=cluster
      String mgmserverRole = "mgmserver";
      String ip = findIpByRole(cluster, service, mgmserverRole);
      WebCommunication webComm = new WebCommunication();
      return webComm.getConfig(ip, cluster, service, mgmserverRole);
   }

   public String getNotAvailable() {
      return NOT_AVAILABLE;
   }

   public boolean getShowNdbInfo() {
      if (service == null) {
         return false;
      }
      if (service.equalsIgnoreCase("mysqlcluster")) {
         return true;
      }
      return false;
   }

   public boolean showKTHFSGraphs() {
      if (service.equals(ServiceType.KTHFS.toString())) {
         return true;
      }
      return false;
   }

   public boolean showMySQLClusterGraphs() {
      if (service.equals(ServiceType.MySQLCluster.toString())) {
         return true;
      }
      return false;
   }


   public String findServiceByRole(String role) {
      return servicesRolesMap.get(role);
   }

   private String findIpByHostname(String hostname) throws Exception {
      Host h = hostEJB.findHostByName(hostname);
      if (h == null) {
         throw new RuntimeException("Hostname " + hostname + " not found.");
      }
      String ip = h.getPrivateIp();
      if (ip == null || ip.equals("")) {
         h.getPublicIp();
      }
      return ip;
   }

   private String findIpByRole(String cluster, String service, String role) throws Exception {
      String host = roleEjb.findRoles(cluster, service, role).get(0).getHostname();
      return findIpByHostname(host);
   }
}