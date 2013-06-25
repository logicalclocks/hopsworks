package se.kth.kthfsdashboard.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.role.RoleType;
import se.kth.kthfsdashboard.role.Status;
import se.kth.kthfsdashboard.struct.Health;
import se.kth.kthfsdashboard.struct.ServiceRoleInfo;

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
      roles.add(new ServiceRoleInfo("NameNode", RoleType.namenode));
      roles.add(new ServiceRoleInfo("DataNode", RoleType.datanode));
      serviceRolesMap.put(ServiceType.KTHFS, roles);

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("MySQL Cluster NDBD (ndb)", RoleType.ndb));
      roles.add(new ServiceRoleInfo("MySQL Server (mysqld)", RoleType.mysqld));
      roles.add(new ServiceRoleInfo("MGM Server (mgmserver)", RoleType.mgmserver));
      serviceRolesMap.put(ServiceType.MySQLCluster, roles);

      roles = new ArrayList<ServiceRoleInfo>();
      roles.add(new ServiceRoleInfo("Resource Manager", RoleType.resourcemanager));
      roles.add(new ServiceRoleInfo("Node Manager", RoleType.nodemanager));
      serviceRolesMap.put(ServiceType.YARN, roles);
   }

   @PostConstruct
   public void init() {
      logger.info("init ServiceStatusController");
      loadRoles();
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

      Long started, stopped, failed, good, bad;
      started = roleEjb.countStatus(cluster, service, serviceRoleInfo.getRoleName(), Status.Started);
      stopped = roleEjb.countStatus(cluster, service, serviceRoleInfo.getRoleName(), Status.Stopped);
      failed = roleEjb.countStatus(cluster, service, serviceRoleInfo.getRoleName(), Status.Failed);
      good = started;
      bad = failed + stopped;
      serviceRoleInfo.setStatusStarted(String.format("%d Started", started));
      serviceRoleInfo.setStatusStopped(String.format("%d Stopped", stopped + failed));
      serviceRoleInfo.setHealth(String.format("%d Good, %d Bad", good, bad));
      if (bad > 0) {
         serviceHealth = Health.Bad;
      }
      return serviceRoleInfo;
   }
}