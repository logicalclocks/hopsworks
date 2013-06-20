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
import se.kth.kthfsdashboard.struct.ClusterInfo;
import se.kth.kthfsdashboard.struct.Health;
import se.kth.kthfsdashboard.struct.InstanceInfo;
import se.kth.kthfsdashboard.struct.ServiceInfo;
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
   private HashMap<String, ClusterInfo> clusters = new HashMap<String, ClusterInfo>();
   private HashMap<String, InstanceInfo> instances = new HashMap<String, InstanceInfo>();
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

   public String requestParams() {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
      Principal principal = request.getUserPrincipal();

      return request.getAuthType().toString() + " - " + principal.getName();
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

   public List<ServiceRoleInfo> getRoles() {

      List<ServiceRoleInfo> serviceRoles = new ArrayList<ServiceRoleInfo>();
      for (ServiceRoleInfo role : rolesMap.get(service)) {
         serviceRoles.add(setStatus(cluster, service, role));
      }
      return serviceRoles;
   }

   public List<ClusterInfo> getClusters() {

      // TODO: Insert correct Info for Service Types, ...

      List<ClusterInfo> allClusters = new ArrayList<ClusterInfo>();
      for (String c : roleEjb.findClusters()) {
         Map services = new HashMap<String, String>();
         Map rolesHealth = new HashMap<String, String>();
         Long numberOfHosts = roleEjb.countClusterMachines(c);
         ClusterInfo clusterInfo = new ClusterInfo(c, numberOfHosts);
         List<Role> roles = roleEjb.findRoles(c);
         int startedRoles = 0;
         int stoppedRoles = 0;
         for (Role r : roles) {
            if (r.getStatus() == Status.Started) {
               if (!services.containsKey(r.getService())) {
                  services.put(r.getService(), Health.None.toString());
               }
               if (!rolesHealth.containsKey(r.getRole())) {
                  rolesHealth.put(r.getRole(), Health.None.toString());
               }
               startedRoles += 1;
            } else {
               services.put(r.getService(), Health.Bad.toString());
               rolesHealth.put(r.getRole(), Health.Bad.toString());
               stoppedRoles += 1;
            }
            if (clusterInfo.getRoleCounts().containsKey(r.getRole())) {
               Integer count = (Integer) clusterInfo.getRoleCounts().get(r.getRole());
               clusterInfo.putToRoleCounts(r.getRole(), count + 1);
            } else {
               clusterInfo.putToRoleCounts(r.getRole(), 1);
            }

         }
         String clusterStatus = startedRoles + " Started, " + stoppedRoles + " Stopped";
         String clusterHealth = (stoppedRoles == 0) ? Health.Good.toString() : Health.Bad.toString();
         clusterInfo.setServices(services);
         clusterInfo.setRolesHealth(rolesHealth);
         clusterInfo.setHealth(clusterHealth);
         clusterInfo.setStatus(clusterStatus);
         allClusters.add(clusterInfo);
      }
      return allClusters;
   }

   public List<ServiceInfo> getServices() {

      List<ServiceInfo> services = new ArrayList<ServiceInfo>();
      for (String s : roleEjb.findServices(cluster)) {
         List<Role> roles = roleEjb.findRoles(cluster, s);
         ServiceInfo serviceInfo = new ServiceInfo(s);
         int started = 0;
         int stopped = 0;
         for (Role r : roles) {
            if (r.getStatus() == Status.Started) {
               started += 1;
            } else {
               stopped += 1;
            }
            if (serviceInfo.getRoleCounts().containsKey(r.getRole())) {
               Integer count = (Integer) serviceInfo.getRoleCounts().get(r.getRole());
               serviceInfo.putToRoleCounts(r.getRole(), count + 1);
            } else {
               serviceInfo.putToRoleCounts(r.getRole(), 1);
            }
         }
         String serviceStatus = started + " Started, " + stopped + " Stopped";
         String serviceHealth = (stopped == 0) ? Health.Good.toString() : Health.Bad.toString();
         serviceInfo.setStatus(serviceStatus);
         serviceInfo.setHealth(serviceHealth);
         services.add(serviceInfo);
      }
      return services;
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