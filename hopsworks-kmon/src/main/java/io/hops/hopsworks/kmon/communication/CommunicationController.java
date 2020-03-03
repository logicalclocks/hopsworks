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

package io.hops.hopsworks.kmon.communication;

import io.hops.hopsworks.common.util.WebCommunication;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.kmon.group.ServiceInstancesController;
import io.hops.hopsworks.kmon.struct.InstanceInfo;
import java.util.concurrent.Future;
import java.util.logging.Level;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class CommunicationController {

  @EJB
  private HostsFacade hostEJB;
  @EJB
  private WebCommunication web;

  @ManagedProperty(value = "#{serviceInstancesController}")
  private ServiceInstancesController serviceInstancesController;

  @ManagedProperty("#{param.group}")
  private String group; 
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.hostname}")
  private String hostname;

  private List<InstanceInfo> instances;

  private static final Logger LOGGER = Logger.getLogger(CommunicationController.class.getName());

  public CommunicationController() {
    LOGGER.log(Level.FINE, "CommunicationController: hostname: " + hostname + " ; group: " + group
        + " ; service: " + service);
  }

  @PostConstruct
  public void init() {
  }

  public String getService() {
    return service;
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

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    if (hostname == null || hostname.compareTo("null")==0) {
      return;
    }
    this.hostname = hostname;
  }
  
  public ServiceInstancesController getServiceInstancesController() {
    return serviceInstancesController;
  }
  
  public void setServiceInstancesController(ServiceInstancesController serviceInstancesController) {
    this.serviceInstancesController = serviceInstancesController;
  }
  
  private Hosts findHostByName(String hostname) throws Exception {
    return hostEJB.findByHostname(hostname).orElseThrow(() ->
      new RuntimeException("Hostname " + hostname + " not found."));
  }

  private void uiMsg(String res) {
    FacesContext context = FacesContext.getCurrentInstance();
    FacesMessage msg = null;
    if (res.contains("Error")) {
      msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, res,
          "There was a problem when executing the operation.");
    } else {
      msg = new FacesMessage(FacesMessage.SEVERITY_INFO, res,
          "Successfully executed the operation.");
    }
    context.addMessage(null, msg);
  }

  public void serviceStart() {
    uiMsg(serviceOperation("startService"));

  }

  public void serviceStartAll() {
    uiMsg(serviceOperationAll("startService"));
  }

  public void serviceRestart() {
    uiMsg(serviceOperation("restartService"));
  }

  public void serviceRestartAll() {
    uiMsg(serviceOperationAll("restartService"));
  }

  public void serviceStop() {
    uiMsg(serviceOperation("stopService"));
  }

  public void serviceStopAll() {
    LOGGER.log(Level.SEVERE, "serviceStopAll 1");
    uiMsg(serviceOperationAll("stopService"));
  }

  private String serviceOperationAll(String operation) {
    instances = serviceInstancesController.getInstances();
    List<Future<String>> results = new ArrayList<>();
    String result = "";
    for (InstanceInfo instance : instances) {
      if (instance.getService().equals(service)) {
        try {
          Hosts h = findHostByName(instance.getHost());
          String ip = h.getPublicOrPrivateIp();
          String agentPassword = h.getAgentPassword();
          results.add(web.asyncServiceOp(operation, ip, agentPassword, group, service));
        } catch (Exception ex) {
          result = result + ex.getMessage() + "\n";
        }
      }
    }
    for (Future<String> r : results) {
      try {
        result = result + r.get() + "\n";
      } catch (Exception ex) {
        result = result + ex.getMessage() + "\n";
      }
    }
    return result;
  }

  private String serviceOperation(String operation) {
    try {
      Hosts h = findHostByName(hostname);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      return web.serviceOp(operation, ip, agentPassword, group, service);
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }
}
