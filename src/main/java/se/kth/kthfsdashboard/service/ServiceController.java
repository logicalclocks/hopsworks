package se.kth.kthfsdashboard.service;

import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.role.Role;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.Role.RoleType;
import se.kth.kthfsdashboard.struct.InstanceInfo;
import se.kth.kthfsdashboard.struct.ClusterInfo;
import se.kth.kthfsdashboard.struct.InstanceFullInfo;
import se.kth.kthfsdashboard.struct.ServiceRoleInfo;
import se.kth.kthfsdashboard.util.Formatter;
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
   private HashMap<String, ClusterInfo> clusters = new HashMap<String, ClusterInfo>();
   private HashMap<String, InstanceInfo> instances = new HashMap<String, InstanceInfo>();
   private static Logger log = Logger.getLogger(ServiceController.class.getName());
   public static String NOT_AVAILABLE = "Not available.";
   public static Map<String, List<ServiceRoleInfo>> rolesMap = new HashMap<String, List<ServiceRoleInfo>>();
   public static Map<String, String> servicesRolesMap = new HashMap<String, String>();

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

   public List<ClusterInfo> getClusters() {

      // TODO: Insert correct Info for Service Types, ...
      
      List<ClusterInfo> allClusters = new ArrayList<ClusterInfo>();
      for (String c : roleEjb.findClusters()) {
         ClusterInfo clusterInfo = new ClusterInfo(c, "-", "-");
         List<Role> roles = roleEjb.findRoles(c);
         for (Role r : roles) {
            if (clusterInfo.getRoleCounts().containsKey(r.getRole())) {
               Integer count = (Integer) clusterInfo.getRoleCounts().get(r.getRole());
               clusterInfo.putToRoleCounts(r.getRole(), count + 1);
            } else {
               clusterInfo.putToRoleCounts(r.getRole(), 1);
            }
         }
         List<String> serviceClasses = roleEjb.findServices(c);
         clusterInfo.setServices(serviceClasses);

         allClusters.add(clusterInfo);
      }
      return allClusters;
   }

   public String requestParams() {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
      Principal principal = request.getUserPrincipal();

      return request.getAuthType().toString() + " - " + principal.getName();
   }

   public List<InstanceFullInfo> getInstanceFullInfo() {
      List<InstanceFullInfo> instanceInfoList = new ArrayList<InstanceFullInfo>();
      List<Role> roles = roleEjb.findByHostnameClusterRole(hostname, cluster, role);
      for (Role r : roles) {
         String ip = hostEJB.findHostByName(hostname).getPublicIp();
         InstanceFullInfo i = new InstanceFullInfo(r.getCluster(), r.getService(), r.getRole(), r.getHostname(), ip, r.getWebPort(), "?", r.getStatus(), r.getHealth().toString());
         i.setPid(r.getPid());
         i.setUptime(Formatter.time(r.getUptime() * 1000));
         instanceInfoList.add(i);
      }
      return instanceInfoList;
   }

   public String doGotoClusterStatus() {
      return "cluster-status?faces-redirect=true&cluster=" + cluster;
   }

   public String gotoServiceInstance() {
      return "services-instances-status?faces-redirect=true&hostname="
              + hostname + "&cluster=" + cluster + "&role=" + role;
   }

   public String gotoClusterStatus() {
      return "cluster-status?faces-redirect=true&cluster=" + cluster;
   }

   public String gotoClusterCommandHistory() {
      return "cluster-commands?faces-redirect=true&cluster=" + cluster;
   }

   public String gotoServiceStatus() {
      return "service-status?faces-redirect=true&cluster=" + cluster + "&service=" + service;
   }

   public String gotoServiceInstances() {
      String url = "service-instances?faces-redirect=true";
      if (hostname != null) {
         url += "&hostname=" + hostname;
      }
      if (cluster != null) {
         url += "&cluster=" + cluster;
      }
      if (service != null) {
         url += "&service=" + service;
      }
      if (role != null) {
         url += "&role=" + role;
      }
      if (status != null) {
         url += "&status=" + status;
      }
      return url;
   }

   public String gotoServiceCommandHistory() {
      return "service-commands?faces-redirect=true&cluster=" + cluster + "&service=" + service;
   }

   public String gotoRole() {
      return "role?faces-redirect=true&hostname=" + hostname + "&cluster=" + cluster
              + "&service=" + service + "&role=" + role;
   }

   public String getRoleUrl() {
      return "role.xhtml";
   }

   public String getHostUrl() {
      return "host.xhtml";
   }

   public List<ServiceRoleInfo> getRoles() {

      List<ServiceRoleInfo> serviceRoles = new ArrayList<ServiceRoleInfo>();
      for (ServiceRoleInfo role : rolesMap.get(service)) {
         serviceRoles.add(setStatus(cluster, service, role));
      }
      return serviceRoles;
   }

   public List<String> getServices() {

      List<String> serviceRoles = new ArrayList<String>();
      for (String s : roleEjb.findServices(cluster)) {
         serviceRoles.add(s);
      }
      return serviceRoles;
   }

   public List<ServiceRoleInfo> getSuberviceRoles() {

      List<ServiceRoleInfo> serviceRoles = new ArrayList<ServiceRoleInfo>();
      if (service != null) { // service = mysqlcluster
         for (ServiceRoleInfo r : rolesMap.get(service)) {
            serviceRoles.add(setStatus(cluster, service, r));
         }
      }
      return serviceRoles;
   }

   private ServiceRoleInfo setStatus(String cluster, String group, ServiceRoleInfo role) {
      int started, stopped, failed, good, bad;
      started = roleEjb.countStatus(cluster, group, role.getShortName(), Role.Status.Started);
      stopped = roleEjb.countStatus(cluster, group, role.getShortName(), Role.Status.Stopped);
      failed = roleEjb.countStatus(cluster, group, role.getShortName(), Role.Status.Failed);
//      good = started + stopped;
//      bad = failed;
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
      Role s = roleEjb.find(hostname, cluster, service, role);
      if (s.getWebPort() == null || s.getWebPort() == 0) {
         return false;
      }
      return true;
   }

   public String getRoleLog(int lines) {
      WebCommunication webComm = new WebCommunication();
      return webComm.getRoleLog(hostname, cluster, service, role, lines);
   }
   
   public String getServiceLog(int lines) {
      final String ROLE = "mgmserver";
      String host = roleEjb.findRoles(cluster, service, ROLE).get(0).getHostname();      
      WebCommunication webComm = new WebCommunication();
      return webComm.getServiceLog(host, cluster, service, lines);
   }   

   public String getAgentLog(int lines) {
      WebCommunication webCom = new WebCommunication();
      return webCom.getAgentLog(hostname, lines);
   }

   public String getMySQLClusterConfig() throws Exception {

      // Finds hostname of mgmserver
      // Role=mgmserver , Service=MySQLCluster, Cluster=cluster
      final String ROLE = "mgmserver";
      String host = roleEjb.findRoles(cluster, service, ROLE).get(0).getHostname();
      WebCommunication webComm = new WebCommunication();
      return webComm.getConfig(host, cluster, service, ROLE);
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

   public String findServiceByRole(String role) {
      return servicesRolesMap.get(role);
   }
}