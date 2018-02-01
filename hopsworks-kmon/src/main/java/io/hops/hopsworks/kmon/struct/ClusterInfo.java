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

package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.kagent.HostServicesInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import io.hops.hopsworks.common.util.FormatUtils;

public class ClusterInfo {

  private String name;
  private Long numberOfHosts;
  private Long totalCores;
  private Long totalMemoryCapacity;
  private Long totalDiskCapacity;
  private Set<String> groups = new HashSet<>();
  private Set<String> services = new HashSet<>();
  private Set<String> badServices = new HashSet<>();
  private Map<String, Integer> servicesCount = new TreeMap<>();
  private Map<String, String> servicesGroupsMap = new TreeMap<>();
  private Integer started, stopped, timedOut;

  public ClusterInfo(String name) {
    started = 0;
    stopped = 0;
    timedOut = 0;
    this.name = name;
  }

  public void setNumberOfHosts(Long numberOfHosts) {
    this.numberOfHosts = numberOfHosts;
  }

  public Long getNumberOfHosts() {
    return numberOfHosts;
  }

  public String getName() {
    return name;
  }

  public String[] getGroups() {
    return groups.toArray(new String[groups.size()]);
  }

  public String[] getServices() {
    return services.toArray(new String[services.size()]);
  }

  public Long getTotalCores() {
    return totalCores;
  }

  public void setTotalCores(Long totalCores) {
    this.totalCores = totalCores;
  }

  public Integer serviceCount(String service) {
    return servicesCount.get(service);
  }

  /** Returns the group for a service 
   **/
  public String serviceGroup(String service) {
    return servicesGroupsMap.get(service);
  }

  public Health getClusterHealth() {
    if (badServices.isEmpty()) {
      return Health.Good;
    }
    return Health.Bad;
  }

  public Health groupHealth(String group) {
    if (badServices.contains(group)) {
      return Health.Bad;
    }
    return Health.Good;
  }

  public Health serviceHealth(String service) {
    if (badServices.contains(service)) {
      return Health.Bad;
    }
    return Health.Good;
  }

  public Map getStatus() {

    Map<Status, Integer> statusMap = new TreeMap<Status, Integer>();
    if (started > 0) {
      statusMap.put(Status.Started, started);
    }
    if (stopped > 0) {
      statusMap.put(Status.Stopped, stopped);
    }
    if (timedOut > 0) {
      statusMap.put(Status.TimedOut, timedOut);
    }
    return statusMap;
  }

  public void addServices(List<HostServicesInfo> serviceHostList) {
    for (HostServicesInfo serviceHost : serviceHostList) {
      groups.add(serviceHost.getHostServices().getGroup());
      if (serviceHost.getHostServices().getService().toString().equals("")) {
        continue;
      }
      services.add(serviceHost.getHostServices().getService());
      servicesGroupsMap.put(serviceHost.getHostServices().getService(), serviceHost.getHostServices().
          getGroup());
      if (serviceHost.getStatus() == Status.Started) {
        started += 1;
      } else {
        badServices.add(serviceHost.getHostServices().getService());
        badServices.add(serviceHost.getHostServices().getService());
        if (serviceHost.getStatus() == Status.Stopped) {
          stopped += 1;
        } else if (serviceHost.getStatus() == Status.TimedOut) {
          timedOut += 1;
        }
      }
      addService(serviceHost.getHostServices().getService());
    }
  }

  private void addService(String service) {
    if (servicesCount.containsKey(service)) {
      Integer current = servicesCount.get(service);
      servicesCount.put(service, current + 1);
    } else {
      servicesCount.put(service, 1);
    }
  }

  public String getTotalMemoryCapacity() {
    if (totalMemoryCapacity == null) {
      return "N/A";
    }
    return FormatUtils.storage(totalMemoryCapacity);
  }

  public void setTotalMemoryCapacity(Long totalMemoryCapacity) {
    this.totalMemoryCapacity = totalMemoryCapacity;
  }

  public String getTotalDiskCapacity() {
    if (totalDiskCapacity == null) {
      return "N/A";
    }
    return FormatUtils.storage(totalDiskCapacity);
  }

  public void setTotalDiskCapacity(Long totalDiskCapacity) {
    this.totalDiskCapacity = totalDiskCapacity;
  }
}
