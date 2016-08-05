package io.hops.kmon.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import io.hops.kmon.role.RoleEJB;
import io.hops.kmon.struct.ClusterInfo;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class ClustersController {

   @EJB
   private RoleEJB roleEjb;
   private static final Logger logger = Logger.getLogger(ClustersController.class.getName());
   private List<ClusterInfo> clusters;

   public ClustersController() {
   }

   @PostConstruct
   public void init() {
      logger.info("init ClustersController");
      clusters = new ArrayList<>();
      loadClusters();
   }

   public List<ClusterInfo> getClusters() {
      return clusters;
   }

   private void loadClusters() {
      for (String cluster : roleEjb.findClusters()) {  
         ClusterInfo clusterInfo = new ClusterInfo(cluster);
         clusterInfo.setNumberOfHost(roleEjb.countHosts(cluster));
         clusterInfo.setTotalCores(roleEjb.totalCores(cluster));
         clusterInfo.setTotalMemoryCapacity(roleEjb.totalMemoryCapacity(cluster));
         clusterInfo.setTotalDiskCapacity(roleEjb.totalDiskCapacity(cluster));
         clusterInfo.addRoles(roleEjb.findRoleHost(cluster));
         clusters.add(clusterInfo);
      }
   }
}