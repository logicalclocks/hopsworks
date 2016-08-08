package io.hops.kmon.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.kmon.role.RoleEJB;
import io.hops.kmon.struct.ClusterInfo;
import io.hops.kmon.struct.Health;
import io.hops.kmon.struct.ServiceInfo;

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
  private List<ServiceInfo> services = new ArrayList<>();
  private Health clusterHealth;
  private boolean found;
  private ClusterInfo clusterInfo;

  public ClusterStatusController() {
  }

  @PostConstruct
  public void init() {
    logger.info("init ClusterStatusController");
        loadServices();
        loadCluster();
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
    loadCluster();
    return clusterHealth;
  }

  public void loadServices() {
    clusterHealth = Health.Good;
    List<String> servicesList = roleEjb.findServices(cluster);
    if (!servicesList.isEmpty()) {
      found = true;
    }
    for (String s : servicesList) {
      ServiceInfo serviceInfo = new ServiceInfo(s);
      Health health = serviceInfo.addRoles(roleEjb.findRoleHost(cluster, s));
      if (health == Health.Bad) {
        clusterHealth = Health.Bad;
      }
      services.add(serviceInfo);
    }
  }

  private void loadCluster() {
    if (clusterInfo != null) {
      return;
    }
    clusterInfo = new ClusterInfo(cluster);
    clusterInfo.setNumberOfHost(roleEjb.countHosts(cluster));
    clusterInfo.setTotalCores(roleEjb.totalCores(cluster));
    clusterInfo.setTotalMemoryCapacity(roleEjb.totalMemoryCapacity(cluster));
    clusterInfo.setTotalDiskCapacity(roleEjb.totalDiskCapacity(cluster));
    clusterInfo.addRoles(roleEjb.findRoleHost(cluster));
  }

  public ClusterInfo getClusterInfo() {
    loadCluster();
    return clusterInfo;
  }
}
