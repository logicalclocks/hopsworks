package se.kth.kthfsdashboard.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.role.RoleType;
import se.kth.kthfsdashboard.struct.Health;
import se.kth.kthfsdashboard.struct.RoleHostInfo;
import se.kth.kthfsdashboard.struct.ServiceRoleInfo;
import se.kth.kthfsdashboard.struct.Status;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class ServiceStatusController {

   @EJB
   private RoleEJB roleEjb;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   public static Map<ServiceType, List<ServiceRoleInfo>> serviceRolesMap;
   private List<ServiceRoleInfo> serviceRoles = new ArrayList<ServiceRoleInfo>();
   private Health serviceHealth;
   private boolean found;
   private static final Logger logger = Logger.getLogger(ServiceStatusController.class.getName());

   public ServiceStatusController() {
      serviceRolesMap = new EnumMap<ServiceType, List<ServiceRoleInfo>>(ServiceType.class);

      List<ServiceRoleInfo> roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("Name Node", RoleType.namenode));
      roles.add(new ServiceRoleInfo("Data Node", RoleType.datanode));
      serviceRolesMap.put(ServiceType.KTHFS, roles);

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("MySQL Cluster NDB (ndb)", RoleType.ndb));
      roles.add(new ServiceRoleInfo("MySQL Server (mysqld)", RoleType.mysqld));
      roles.add(new ServiceRoleInfo("MGM Server (mgmserver)", RoleType.mgmserver));
      roles.add(new ServiceRoleInfo("Memcached (memcached)", RoleType.memcached));
      serviceRolesMap.put(ServiceType.MySQLCluster, roles);

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("Resource Manager", RoleType.resourcemanager));
      roles.add(new ServiceRoleInfo("Node Manager", RoleType.nodemanager));
      serviceRolesMap.put(ServiceType.YARN, roles);

      roles = new ArrayList<ServiceRoleInfo>();
      serviceRolesMap.put(ServiceType.Spark, roles);   
   
   }

   @PostConstruct
   public void init() {
      logger.info("init ServiceStatusController");
      loadRoles();
      found =true; // TODO: Remove this. Fix it for Spark
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

   public boolean isFound() {
      return found;
   }

   public void setFound(boolean found) {
      this.found = found;
   }

   public Health getHealth() {
      return serviceHealth;
   }

   public List<ServiceRoleInfo> getRoles() {
      return serviceRoles;
   }

   public boolean renderTerminalLink() {
      return service.equalsIgnoreCase(ServiceType.KTHFS.toString()) || 
              service.equalsIgnoreCase(ServiceType.MySQLCluster.toString()) ||
              service.equalsIgnoreCase(ServiceType.Spark.toString());
   }
   
   public boolean renderNdbInfoTable() {
      return service.equals(ServiceType.MySQLCluster.toString());
   }

   public boolean renderLog() {
      return service.equals(ServiceType.MySQLCluster.toString());
   }

   public boolean renderConfiguration() {
      return service.equals(ServiceType.MySQLCluster.toString());
   }

   private void loadRoles() {
      serviceHealth = Health.Good;
      try {
         ServiceType serviceType = ServiceType.valueOf(service);
         for (ServiceRoleInfo serviceRoleInfo : serviceRolesMap.get(serviceType)) {
            serviceRoles.add(setStatus(cluster, service, serviceRoleInfo));
         }
      } catch (Exception ex) {
         logger.log(Level.SEVERE, "Invalid service type: {0}", service);
      }
   }

   private ServiceRoleInfo setStatus(String cluster, String service, ServiceRoleInfo serviceRoleInfo) {

      TreeMap<Status, Integer> statusMap = new TreeMap<Status, Integer>();
      TreeMap<Health, Integer> healthMap = new TreeMap<Health, Integer>();
      List<RoleHostInfo> roleHosts = roleEjb.findRoleHost(cluster, service, serviceRoleInfo.getRoleName());
      if (!roleHosts.isEmpty()) {
         found = true;
      }
      for (RoleHostInfo roleHost : roleHosts) {
         if (statusMap.containsKey(roleHost.getStatus())) {
            statusMap.put(roleHost.getStatus(), statusMap.get(roleHost.getStatus()) + 1);
         } else {
            statusMap.put(roleHost.getStatus(), 1);
         }
         if (healthMap.containsKey(roleHost.getHealth())) {
            Integer count = healthMap.get(roleHost.getHealth()) + 1;
            healthMap.put(roleHost.getHealth(), count);
         } else {
            healthMap.put(roleHost.getHealth(), 1);
         }
      }
      serviceRoleInfo.setStatusMap(statusMap);
      serviceRoleInfo.setHealthMap(healthMap);
      if (healthMap.containsKey(Health.Bad)) {
         serviceHealth = Health.Bad;
      }
      return serviceRoleInfo;
   }
}