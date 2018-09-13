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
