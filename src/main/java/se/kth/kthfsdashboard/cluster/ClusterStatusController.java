package se.kth.kthfsdashboard.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.role.RoleEJB;
import se.kth.kthfsdashboard.struct.Health;
import se.kth.kthfsdashboard.struct.ServiceInfo;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class ClusterStatusController {

   @EJB
   private RoleEJB roleEjb;
   @ManagedProperty("#{param.cluster}")
   private String cluster;   
   private static final Logger logger = Logger.getLogger(ClusterStatusController.class.getName());
   private List<ServiceInfo> services;
   private Health clusterHealth;
   private boolean found;
   
   public ClusterStatusController() {
   }

   @PostConstruct
   public void init() {
      logger.info("init ClusterStatusController");
      services = new ArrayList<ServiceInfo>();
      loadServices();
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

   public List<ServiceInfo> getServices() {
      return services;
   }
   
   public Health getClusterHealth() {
      return clusterHealth;
   }

   public void loadServices() {
      clusterHealth = Health.Good;
      List <String> servicesList = roleEjb.findServices(cluster);
      if (!servicesList.isEmpty()) {
         found = true;
      }
      for (String s : servicesList ) {
         ServiceInfo serviceInfo = new ServiceInfo(s);
         Health health = serviceInfo.addRoles(roleEjb.findRoleHost(cluster, s));
         if (health == Health.Bad) {
            clusterHealth = Health.Bad;
         }
         services.add(serviceInfo);
      }
   }
  
}