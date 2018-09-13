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

package io.hops.hopsworks.kmon.group;

import io.hops.hopsworks.kmon.struct.GroupType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.kmon.service.GroupServiceMapper;
import io.hops.hopsworks.kmon.struct.ServiceType;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesInfo;
import io.hops.hopsworks.kmon.struct.ServiceInstancesInfo;

@ManagedBean
@RequestScoped
public class GroupStatusController {

  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.group}")
  private String group;

  @EJB
  private HostServicesFacade hostServicesFacade;

  private Health health;
  private List<ServiceInstancesInfo> groupServices = new ArrayList<ServiceInstancesInfo>();
  private static final Logger logger = Logger.getLogger(GroupStatusController.class.getName());

  public GroupStatusController() {
  }

  @PostConstruct
  public void init() {
    logger.info("GroupStatusController: cluster: " + cluster + "; group: " + group);
    loadServices();
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public Health getHealth() {
    return health;
  }

  public List<ServiceInstancesInfo> getServices() {
    return groupServices;
  }

  public boolean renderTerminalLink() {
    return group.equalsIgnoreCase(GroupType.HDFS.toString())
        || group.equalsIgnoreCase(GroupType.NDB.toString());
  }

  public boolean renderInstancesLink() {
//    return !group.equalsIgnoreCase(GroupType.Spark.toString());
    return true;
  }

  public boolean renderNdbInfoTable() {
    return group.equals(GroupType.NDB.toString());
  }

  public boolean renderLog() {
    return group.equals(GroupType.NDB.toString());
  }

  public boolean renderConfiguration() {
    return group.equals(GroupType.NDB.toString());
  }

  private void loadServices() {
    health = Health.Good;
    try {
      for (ServiceType service : GroupServiceMapper.getServices(group)) {
        groupServices.add(createServiceInstancesInfo(cluster, group, service));
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Invalid service type: {0}", group);
    }
  }

  private ServiceInstancesInfo createServiceInstancesInfo(String cluster, String group, ServiceType service) {
    ServiceInstancesInfo groupInstancesInfo = new ServiceInstancesInfo(GroupServiceMapper.getServiceFullName(service),
        service);
    List<HostServicesInfo> serviceHosts = hostServicesFacade.findHostServices(cluster, group, service.toString());
    for (HostServicesInfo serviceHost : serviceHosts) {
      groupInstancesInfo.addInstanceInfo(serviceHost.getStatus(), serviceHost.getHealth());
    }
    if (groupInstancesInfo.getOverallHealth() == Health.Bad) {
      health = Health.Bad;
    }
    return groupInstancesInfo;
  }

}
