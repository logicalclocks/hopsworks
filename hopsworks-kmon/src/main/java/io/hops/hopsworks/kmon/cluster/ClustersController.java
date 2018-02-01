/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
