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

import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.kmon.struct.InstanceInfo;
import io.hops.hopsworks.persistence.entity.host.ServiceStatus;
import io.hops.hopsworks.persistence.entity.kagent.HostServices;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@RequestScoped
public class ServiceInstancesController {

  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.status}")
  private String status;
  @EJB
  private HostServicesFacade hostServicesFacade;
  private List<InstanceInfo> filteredInstances = new ArrayList<>();
  private static final Logger LOGGER = Logger.getLogger(ServiceInstancesController.class.getName());

  public ServiceInstancesController() {
    LOGGER.log(Level.FINE, "ServiceInstancesController: status: " + status + " ; " + group
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<InstanceInfo> getInstances() {
//      With prettyfaces, parameters (clusters, service, service) will not be null.
//      Without prettyfaces, parameters will be null when filter is changed, they
//      should be stored in cookie
    List<InstanceInfo> instances = new ArrayList<>();
    List<HostServices> serviceHostList = new ArrayList<>();
    if (group != null && service != null && status != null) {
      for (HostServices hostServicesInfo : hostServicesFacade.findServices(service)) {
        if (hostServicesInfo.getStatus() == ServiceStatus.valueOf(status)) {
          serviceHostList.add(hostServicesInfo);
        }
      }
    } else if (group != null && service != null && service.compareTo("null") != 0) {
      serviceHostList = hostServicesFacade.findServices(service);
    } else if (group != null) {
      serviceHostList = hostServicesFacade.findGroupServices(group);
    } else {
      serviceHostList = hostServicesFacade.findAll();
    }

    for (HostServices hsi : serviceHostList) {
      instances.add(new InstanceInfo(hsi.getGroup(), hsi.getName(), hsi.getHost().getHostname(),
          hsi.getStatus(), hsi.getHealth().toString()));
    }
    filteredInstances.addAll(instances);
    return instances;
  }

  public boolean disableStart() {
    List<InstanceInfo> instances = getInstances();
    if (!instances.isEmpty()) {
      for (InstanceInfo instance : instances) {
        if (instance.getStatus() == ServiceStatus.Stopped) {
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
        if (instance.getStatus() == ServiceStatus.Started) {
          return false;
        }
      }
    }
    return true;
  }
}
