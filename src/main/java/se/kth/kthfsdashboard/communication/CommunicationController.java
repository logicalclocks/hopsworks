package se.kth.kthfsdashboard.communication;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.service.*;
import se.kth.kthfsdashboard.struct.NodesTableItem;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class CommunicationController {

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
   private static final Logger logger = Logger.getLogger(ServiceStatusController.class.getName());

   public CommunicationController() {
      logger.info("CommunicationController");
   }

   @PostConstruct
   public void init() {
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

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getCluster() {
      return cluster;
   }

   public String getHostname() {
      return hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public String serviceLog(int lines) {
      try {
         String ip = findIpByRole(cluster, service, "mgmserver");
         WebCommunication webComm = new WebCommunication();
         return webComm.getServiceLog(ip, cluster, service, lines);
      } catch (Exception ex) {
         return ex.getMessage();
      }
   }

   public String mySqlClusterConfig() throws Exception {
      // Finds hostname of mgmserver
      // Role=mgmserver , Service=MySQLCluster, Cluster=cluster
      String mgmserverRole = "mgmserver";
      String ip = findIpByRole(cluster, service, mgmserverRole);
      WebCommunication webComm = new WebCommunication();
      return webComm.getConfig(ip, cluster, service, mgmserverRole);
   }

   private String findIpByRole(String cluster, String service, String role) throws Exception {
      String host = roleEjb.findRoles(cluster, service, role).get(0).getHostname();
      return findIpByHostname(host);
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

   private String findIpByHostname(String hostname) throws Exception {
      try {
         Host host = hostEJB.findHostByName(hostname);
         String privateIp = host.getPrivateIp();
         if (privateIp == null || privateIp.equals("")) {
            return host.getPublicIp();
         }
         return privateIp;
      } catch (Exception ex) {
         throw new RuntimeException("Hostname " + hostname + " not found.");
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

   public List<NodesTableItem> getNdbinfoNodesTable() throws Exception {

      // Finds hostname of mysqld
      // Role=mysqld , Service=MySQLCluster, Cluster=cluster
      final String ROLE = "mysqld";
      List<NodesTableItem> results;
      try {
         String host = roleEjb.findRoles(cluster, service, ROLE).get(0).getHostname();
         WebCommunication wc = new WebCommunication();
         results = wc.getNdbinfoNodesTable(host);
      } catch (Exception ex) {
         results = new ArrayList<NodesTableItem>();
      }
      return results;

   }

}
