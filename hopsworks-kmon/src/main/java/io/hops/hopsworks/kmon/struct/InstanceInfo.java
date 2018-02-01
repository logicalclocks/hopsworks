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
import java.io.Serializable;

public class InstanceInfo implements Serializable {

  private String name;
  private String host;
  private String cluster;
  private String service;
  private String group;
  private Status status;
  private String health;

  public InstanceInfo(String cluster, String group, String service, String host,
          Status status, String health) {

    this.name = service + " @" + host;
    this.host = host;
    this.cluster = cluster;
    this.group = group;
    this.service = service;
    this.status = status;
    this.health = health;
  }

  public String getName() {
    return name;
  }

  public String getHost() {
    return host;
  }

  public Status getStatus() {
    return status;
  }

  public String getHealth() {
    return health;
  }

  public String getService() {
    return service;
  }

  public String getGroup() {
    return group;
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

}
