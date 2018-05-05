/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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
    clusterInfo.setTotalGPUs(hostServicesFacade.totalGPUs(cluster));
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
