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
   private static final Logger logger = Logger.getLogger(ServiceStatusController.class.getName());

   public ServiceStatusController() {
      serviceRolesMap = new EnumMap<ServiceType, List<ServiceRoleInfo>>(ServiceType.class);

      List<ServiceRoleInfo> roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("Name Node", RoleType.namenode));
      roles.add(new ServiceRoleInfo("Data Node", RoleType.datanode));
      serviceRolesMap.put(ServiceType.KTHFS, roles);

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("MySQL Cluster NDBD (ndb)", RoleType.ndb));
      roles.add(new ServiceRoleInfo("MySQL Server (mysqld)", RoleType.mysqld));
      roles.add(new ServiceRoleInfo("MGM Server (mgmserver)", RoleType.mgmserver));
      roles.add(new ServiceRoleInfo("Memcached (memcached)", RoleType.memcached));      
      serviceRolesMap.put(ServiceType.MySQLCluster, roles);

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("Resource Manager", RoleType.resourcemanager));
      roles.add(new ServiceRoleInfo("Node Manager", RoleType.nodemanager));
      serviceRolesMap.put(ServiceType.YARN, roles);
   }

   @PostConstruct
   public void init() {
      logger.info("init ServiceStatusController");
      Long t1, t2;
      t1 = System.currentTimeMillis();
      loadRoles();
      t2 = System.currentTimeMillis();
      logger.log(Level.INFO, "ServiceStatusController >> loadRoles() took {0} ms", t2 - t1);
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

   public Health getHealth() {
      return serviceHealth;
   }

   public List<ServiceRoleInfo> getRoles() {
      return serviceRoles;
   }

   public boolean renderKTHFSGraphs() {
      return service.equals(ServiceType.KTHFS.toString());
   }

   public boolean renderMySQLClusterGraphs() {
      return service.equals(ServiceType.MySQLCluster.toString());
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
      for (ServiceRoleInfo serviceRoleInfo : serviceRolesMap.get(ServiceType.valueOf(service))) {
         serviceRoles.add(setStatus(cluster, service, serviceRoleInfo));
      }
   }

   private ServiceRoleInfo setStatus(String cluster, String service, ServiceRoleInfo serviceRoleInfo) {

      TreeMap<Status, Integer> statusMap = new TreeMap<Status, Integer>();
      TreeMap<Health, Integer> healthMap = new TreeMap<Health, Integer>();
      for (RoleHostInfo roleHost : roleEjb.findRoleHost(cluster, service, serviceRoleInfo.getRoleName())) {
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