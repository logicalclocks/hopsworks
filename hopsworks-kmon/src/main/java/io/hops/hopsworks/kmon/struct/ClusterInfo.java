/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.util.FormatUtils;

public class ClusterInfo {

  private Long numberOfHosts;
  private Long totalCores;
  private Long totalGPUs;
  private Long totalMemoryCapacity;
  private Set<String> groups = new HashSet<>();
  private Set<String> services = new HashSet<>();
  private Set<String> badServices = new HashSet<>();
  private Map<String, Integer> servicesCount = new TreeMap<>();
  private Map<String, String> servicesGroupsMap = new TreeMap<>();
  private Integer started, stopped, timedOut;

  public ClusterInfo() {
    started = 0;
    stopped = 0;
    timedOut = 0;
  }

  public void setNumberOfHosts(Long numberOfHosts) {
    this.numberOfHosts = numberOfHosts;
  }

  public Long getNumberOfHosts() {
    return numberOfHosts;
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

  public Long getTotalGPUs() {
    return totalGPUs;
  }

  public void setTotalGPUs(Long totalGPUs) {
    this.totalGPUs = totalGPUs;
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

  public void addServices(List<HostServices> serviceHostList) {
    for (HostServices serviceHost : serviceHostList) {
      groups.add(serviceHost.getGroup());
      if (serviceHost.getService().equals("")) {
        continue;
      }
      services.add(serviceHost.getService());
      servicesGroupsMap.put(serviceHost.getService(), serviceHost.getGroup());
      if (serviceHost.getStatus() == Status.Started) {
        started += 1;
      } else {
        badServices.add(serviceHost.getService());
        if (serviceHost.getStatus() == Status.Stopped) {
          stopped += 1;
        } else if (serviceHost.getStatus() == Status.TimedOut) {
          timedOut += 1;
        }
      }
      addService(serviceHost.getService());
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
}
