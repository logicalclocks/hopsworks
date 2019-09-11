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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.kmon.struct.ServiceInstancesInfo;

@ManagedBean
@RequestScoped
public class GroupStatusController {

  @ManagedProperty("#{param.group}")
  private String group;

  @EJB
  private HostServicesFacade hostServicesFacade;

  private Health health;
  private List<ServiceInstancesInfo> groupServices = new ArrayList<>();
  private static final Logger LOGGER = Logger.getLogger(GroupStatusController.class.getName());

  public GroupStatusController() {
  }

  @PostConstruct
  public void init() {
    LOGGER.log(Level.FINE, "GroupStatusController: group: " + group);
    loadServices();
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public Health getHealth() {
    return health;
  }

  public List<ServiceInstancesInfo> getServices() {
    return groupServices;
  }

  public boolean renderInstancesLink() {
    return true;
  }

  private void loadServices() {
    health = Health.Good;
    groupServices.addAll(createGroupInstancesInfo(group));
  }

  private Collection<ServiceInstancesInfo> createGroupInstancesInfo(String group) {
    List<HostServices> serviceHosts = hostServicesFacade.findGroupServices(group);
    Map<String, ServiceInstancesInfo> serviceInstancesInfoMap = new HashMap<>();

    for (HostServices hostService : serviceHosts) {
      if (!serviceInstancesInfoMap.containsKey(hostService.getService())) {
        serviceInstancesInfoMap.put(hostService.getService(), new ServiceInstancesInfo(hostService.getService()));
      }

      serviceInstancesInfoMap.get(hostService.getService())
          .addInstanceInfo(hostService.getStatus(), hostService.getHealth());

      if (hostService.getHealth() == Health.Bad) {
        health = Health.Bad;
      }
    }

    return serviceInstancesInfoMap.values();
  }

}
