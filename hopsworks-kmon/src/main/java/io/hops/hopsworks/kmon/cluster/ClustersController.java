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

import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.kmon.struct.ClusterInfo;

@ManagedBean
@RequestScoped
public class ClustersController {

  @EJB
  private HostServicesFacade hostServicesFacade;
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
    for (String cluster : hostServicesFacade.findClusters()) {
      ClusterInfo clusterInfo = new ClusterInfo(cluster);
      clusterInfo.setNumberOfHosts(hostServicesFacade.countHosts(cluster));
      clusterInfo.setTotalCores(hostServicesFacade.totalCores(cluster));
      clusterInfo.setTotalMemoryCapacity(hostServicesFacade.totalMemoryCapacity(cluster));
      clusterInfo.setTotalDiskCapacity(hostServicesFacade.totalDiskCapacity(cluster));
      clusterInfo.addServices(hostServicesFacade.findHostServicesByCluster(cluster));
      clusters.add(clusterInfo);
    }
  }
 
  public String getNameNodesString() {
    StringBuilder hosts = new StringBuilder();
    List<HostServices> hostServices = hostServicesFacade.findServices("namenode");
    if (hostServices != null && !hostServices.isEmpty()) {
      hosts.append(hostServices.get(0).getHost().getHostname());
      for (int i = 1; i < hostServices.size(); i++) {
        hosts.append(",").append(hostServices.get(i).getHost().getHostname());
      }
    }
    return hosts.toString();
  }
}
