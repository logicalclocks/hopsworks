package io.hops.hopsworks.kmon.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.kmon.struct.ClusterInfo; 
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.kmon.struct.GroupInfo;

@ManagedBean
@RequestScoped
public class ClusterStatusController {

  @EJB
  private HostServicesFacade hostServicesFacade;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private static final Logger logger = Logger.getLogger(ClusterStatusController.class.getName());
  private List<GroupInfo> group = new ArrayList<>();
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

  public List<GroupInfo> getGroup() {
    return group;
  }

  public Health getClusterHealth() {
    loadCluster();
    return clusterHealth;
  }

  public void loadServices() {
    clusterHealth = Health.Good;
    List<String> groupList = hostServicesFacade.findGroups(cluster);
    if (!groupList.isEmpty()) {
      found = true;
    }
    for (String s : groupList) {
      GroupInfo groupInfo = new GroupInfo(s);
      Health health = groupInfo.addServices(hostServicesFacade.findHostServicesByGroup(cluster, s));
      if (health == Health.Bad) {
        clusterHealth = Health.Bad;
      }
      group.add(groupInfo);
    }
  }

  private void loadCluster() {
    if (clusterInfo != null) {
      return;
    }
    clusterInfo = new ClusterInfo(cluster);
    clusterInfo.setNumberOfHosts(hostServicesFacade.countHosts(cluster));
    clusterInfo.setTotalCores(hostServicesFacade.totalCores(cluster));
    clusterInfo.setTotalMemoryCapacity(hostServicesFacade.totalMemoryCapacity(cluster));
    clusterInfo.setTotalDiskCapacity(hostServicesFacade.totalDiskCapacity(cluster));
    clusterInfo.addServices(hostServicesFacade.findHostServicesByCluster(cluster));
    found = true;
  }

  public ClusterInfo getClusterInfo() {
    loadCluster();
    return clusterInfo;
  }
}
