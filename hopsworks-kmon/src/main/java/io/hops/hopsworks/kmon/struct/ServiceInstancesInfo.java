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
import java.util.SortedMap;
import java.util.TreeMap;

public class ServiceInstancesInfo {

  private String fullName;
  private String serviceName;
  private SortedMap<Status, Integer> statusMap = new TreeMap<>();
  private SortedMap<Health, Integer> healthMap = new TreeMap<>();

  public ServiceInstancesInfo(String fullName, ServiceType service) {
    this.fullName = fullName;
    this.serviceName = service.toString();
  }

  public String getFullName() {
    return fullName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public Health getServiceHealth() {
    if (!healthMap.containsKey(Health.Bad)) {
      return Health.Good;
    }
    return Health.Bad;
  }

  public String[] getStatusEntries() {
    return statusMap.entrySet().toArray(new String[statusMap.size()]);
  }

  public Integer getStatusCount(Status status) {
    return statusMap.get(status);
  }

  public SortedMap<Status, Integer> getStatusMap() {
    return statusMap;
  }

  public SortedMap<Health, Integer> getHealthMap() {
    return healthMap;
  }

  public void addInstanceInfo(Status status, Health health) {
    if (statusMap.containsKey(status)) {
      statusMap.put(status, statusMap.get(status) + 1);
    } else {
      statusMap.put(status, 1);
    }
    if (healthMap.containsKey(health)) {
      Integer count = healthMap.get(health) + 1;
      healthMap.put(health, count);
    } else {
      healthMap.put(health, 1);
    }
  }

  public Health getOverallHealth() {
    if (healthMap.containsKey(Health.Bad)) {
      return Health.Bad;
    } else {
      return Health.Good;
    }
  }
}
