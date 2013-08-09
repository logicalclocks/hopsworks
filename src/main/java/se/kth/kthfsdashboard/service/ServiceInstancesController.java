package se.kth.kthfsdashboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.InstanceInfo;
import se.kth.kthfsdashboard.struct.RoleHostInfo;
import se.kth.kthfsdashboard.struct.Status;
import se.kth.kthfsdashboard.utils.FilterUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class ServiceInstancesController {
   @ManagedProperty("#{param.r}")
   private String role;
   @ManagedProperty("#{param.service}")
   private String service;
   @ManagedProperty("#{param.cluster}")
   private String cluster;
   @ManagedProperty("#{param.s}")
   private String status;
   @EJB
   private RoleEJB roleEjb;
   private SelectItem[] statusOptions;
   private SelectItem[] hdfsRoleOptions;
   private SelectItem[] healthOptions;
   private SelectItem[] mysqlclusterRoleOptions;
   private SelectItem[] yarnRoleOptions;
   private final static String[] statusStates;
   private final static String[] hdfsRoles;
   private final static String[] mysqlClusterRoles;
   private final static String[] yarnRoles;
   private final static String[] healthStates;
   private List<InstanceInfo> filteredInstances;   
   private static final Logger logger = Logger.getLogger(ServiceInstancesController.class.getName());   
//   private CookieTools cookie = new CookieTools();

   static {
      statusStates = new String[4];
      statusStates[0] = Status.Started.toString();
      statusStates[1] = Status.Stopped.toString();
      statusStates[2] = Status.Failed.toString();
      statusStates[3] = Status.TimedOut.toString();      

      hdfsRoles = new String[]{"namenode", "datanode"};
      mysqlClusterRoles = new String[]{"ndb", "mgmserver", "mysqld", "memcached"};
      yarnRoles = new String[]{"resourcemanager", "nodemanager"};
      healthStates = new String[]{"Good", "Bad"};
   }

   public ServiceInstancesController() {
      
      logger.info("ServiceInstancesController");

      statusOptions = FilterUtils.createFilterOptions(statusStates);
      hdfsRoleOptions = FilterUtils.createFilterOptions(hdfsRoles);
      mysqlclusterRoleOptions = FilterUtils.createFilterOptions(mysqlClusterRoles);
      yarnRoleOptions = FilterUtils.createFilterOptions(yarnRoles);
      healthOptions = FilterUtils.createFilterOptions(healthStates);
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

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }
   
   public List<InstanceInfo> getFilteredInstances() {
      return filteredInstances;
   }

   public void setFilteredInstances(List<InstanceInfo> filteredInstances) {
      this.filteredInstances = filteredInstances;
   }
   
   public SelectItem[] getStatusOptions() {
      return statusOptions;
   }
   
   public SelectItem[] getHealthOptions() {
      return healthOptions;
   }   

   public SelectItem[] getRoleOptions() {
      if (service.equals(ServiceType.KTHFS.toString())) {
         return hdfsRoleOptions;
      } else if (service.equals(ServiceType.MySQLCluster.toString())) {
         return mysqlclusterRoleOptions;
      } else if (service.equals(ServiceType.YARN.toString())) {
         return yarnRoleOptions;
      } else {
         return new SelectItem[]{};
      }
   }   
   
   public List<InstanceInfo> getInstances() {
      
//      with prettyfaces, parameters (clusters, service, role) will not be null.
//      Without prettyfaces, parameters will be null when filter in changed, so
//      we need to store them in cookie
      
      List<InstanceInfo> instances = new ArrayList<InstanceInfo>();
      List<RoleHostInfo> roleHostList = new ArrayList<RoleHostInfo>();
      
      if (cluster != null && role != null && service != null && status != null) {
         for (RoleHostInfo roleHostInfo: roleEjb.findRoleHost(cluster, service, role)){
            if (roleHostInfo.getStatus() == Status.valueOf(status)) {
               roleHostList.add(roleHostInfo);
            }
         }
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);         
      } else if (cluster != null && service != null && role != null) {
         roleHostList = roleEjb.findRoleHost(cluster, service, role);     
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);    
      } else if (cluster != null && service != null) {
         roleHostList = roleEjb.findRoleHost(cluster, service);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);          
      } else if (cluster != null) {
         roleHostList = roleEjb.findRoleHost(cluster);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);             
      }           
//      else {
//         roleHostList = roleEjb.findRoleHost(cookie.read("cluster"), cookie.read("service"));
//      }     
      
      for (RoleHostInfo r : roleHostList) {                
         instances.add(new InstanceInfo(r.getRole().getCluster(), r.getRole().getService(), r.getRole().getRole(),
                 r.getRole().getHostId(), r.getStatus(), r.getHealth().toString()));
      }
      return instances;
   }


}
