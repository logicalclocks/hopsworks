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

package io.hops.hopsworks.kmon.group;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;
import io.hops.hopsworks.kmon.service.GroupServiceMapper;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.kmon.struct.InstanceInfo;
import io.hops.hopsworks.kmon.struct.GroupType;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesInfo;
import io.hops.hopsworks.kmon.utils.FilterUtils;

@ManagedBean
@RequestScoped
public class ServiceInstancesController {

  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.status}")
  private String status;
  @EJB
  private HostServicesFacade hostServicesFacade;
  private static final SelectItem[] statusOptions;
  private static final SelectItem[] healthOptions;
  private List<InstanceInfo> filteredInstances = new ArrayList<>();
  private static final Logger logger = Logger.getLogger(ServiceInstancesController.class.getName());

  private enum groupsWithMetrics {
    HDFS,
    YARN
  };
//   private CookieTools cookie = new CookieTools();

  static {
    statusOptions = FilterUtils.createFilterOptions(Status.values());
    healthOptions = FilterUtils.createFilterOptions(Health.values());
  }

  public ServiceInstancesController() {
    logger.info("ServiceInstancesController: status: " + status + " ; cluster: " + cluster + "; group: " + group
        + " ; service: " + service);

  }

  public String getService() {
    return this.service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public boolean isYarnService() {
    if (group != null) {
      try {
        if (GroupType.valueOf(group).equals(GroupType.YARN)) {
          return true;
        }
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
  }

  public boolean isHDFSService() {
    if (group != null) {
      try {
        if (GroupType.valueOf(group).equals(GroupType.HDFS)) {
          return true;
        }
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
  }

  public boolean getServiceWithMetrics() {
    if (group != null) {
      try {
        groupsWithMetrics.valueOf(group);
        return true;
      } catch (IllegalArgumentException ex) {
      }
    }
    return false;
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

  public SelectItem[] getServiceOptions() {
    try {
      return FilterUtils.createFilterOptions(GroupServiceMapper.getServicesArray(GroupType.valueOf(group)));
    } catch (Exception ex) {
      logger.log(Level.WARNING,"Service not found. Returning no option. Error message: {0}", ex.getMessage());
      return new SelectItem[]{};
    }
  }

  public List<InstanceInfo> getInstances() {
//      With prettyfaces, parameters (clusters, service, service) will not be null.
//      Without prettyfaces, parameters will be null when filter is changed, they
//      should be stored in cookie
    List<InstanceInfo> instances = new ArrayList<InstanceInfo>();
    List<HostServicesInfo> serviceHostList = new ArrayList<HostServicesInfo>();
    if (cluster != null && group != null && service != null && status != null) {
      for (HostServicesInfo hostServicesInfo : hostServicesFacade.findHostServices(cluster, group, service)) {
        if (hostServicesInfo.getStatus() == Status.valueOf(status)) {
          serviceHostList.add(hostServicesInfo);
        }
      }
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);         
    } else if (cluster != null && group != null && service != null && service.compareTo("null") != 0) {
      serviceHostList = hostServicesFacade.findHostServices(cluster, group, service);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);    
    } else if (cluster != null && group != null) {
      serviceHostList = hostServicesFacade.findHostServicesByGroup(cluster, group);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);          
    } else if (cluster != null) {
      serviceHostList = hostServicesFacade.findHostServicesByCluster(cluster);
//         cookie.write("cluster", cluster);
//         cookie.write("service", service);             
    }
//      else {
//         roleHostList = roleEjb.findRoleHost(cookie.read("cluster"), cookie.read("service"));
//      }     
    for (HostServicesInfo hsi : serviceHostList) {
      instances.add(new InstanceInfo(hsi.getHostServices().getCluster(), hsi.getHostServices().
          getGroup(), hsi.getHostServices().getService(), hsi.getHost().getHostname(), 
          hsi.getStatus(), hsi.getHealth().toString()));
    }
    filteredInstances.addAll(instances);
    return instances;
  }

  public boolean disableStart() {
    List<InstanceInfo> instances = getInstances();
    if (!instances.isEmpty()) {
      for (InstanceInfo instance : instances) {
        if (instance.getStatus() == Status.Stopped) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean disableStop() {
    List<InstanceInfo> instances = getInstances();
    if (!instances.isEmpty()) {
      for (InstanceInfo instance : instances) {
        if (instance.getStatus() == Status.Started) {
          return false;
        }
      }
    }
    return true;
  }
}
