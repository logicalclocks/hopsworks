package io.hops.hopsworks.kmon.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.role.RoleEJB;
import io.hops.hopsworks.common.dao.role.Roles;
import io.hops.hopsworks.kmon.struct.ClusterInfo;

@ManagedBean
@RequestScoped
public class ClustersController {

  @EJB
  private RoleEJB roleEjb;
  private static final Logger LOGGER = Logger.getLogger(ClustersController.class.getName());
  private List<ClusterInfo> clusters;

  public ClustersController() {
  }

  @PostConstruct
  public void init() {
    LOGGER.info("init ClustersController");
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
 
  public String getNameNodesString() {
    String hosts = "";
    List<Roles> roles = roleEjb.findRoles("namenode");
    if (roles != null && !roles.isEmpty()) {
      hosts = hosts + roles.get(0).getHost();
      for (int i = 1; i < roles.size(); i++) {
        hosts = hosts + "," + roles.get(i).getHost();
      }
    }
    return hosts;
  }
}
