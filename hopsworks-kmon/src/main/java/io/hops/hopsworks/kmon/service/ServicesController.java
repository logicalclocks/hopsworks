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
package io.hops.hopsworks.kmon.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import io.hops.hopsworks.common.agent.AgentLivenessMonitor;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.util.RemoteCommandResult;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.kmon.struct.InstanceInfo;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.util.FormatUtils;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class ServicesController {

  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private AgentLivenessMonitor agentLivenessMonitor;
  @ManagedProperty("#{param.hostname}")
  private String hostname;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  private List<InstanceInfo> instanceInfoList = new ArrayList<>();
  private String health;
  private boolean found;
  private static final Logger logger = Logger.getLogger(ServicesController.class.
      getName());

  public ServicesController() {
  }

  @PostConstruct
  public void init() {
  }

  public void loadServices() {

    instanceInfoList.clear();
    logger.info("init ServicesController");
    try {
      HostServices serviceHost = hostServicesFacade.find(hostname, group, service);
      String ip = serviceHost.getHost().getPublicOrPrivateIp();
      InstanceInfo info =
          new InstanceInfo(serviceHost.getGroup(), serviceHost.getService(), serviceHost.getHost().getHostname(),
              ip, serviceHost.getStatus(), serviceHost.getHealth().toString());
      info.setPid(serviceHost.getPid());
      String upTime = serviceHost.getHealth() == Health.Good ? FormatUtils.time(serviceHost.getUptime() * 1000) : "";
      info.setUptime(upTime);
      instanceInfoList.add(info);
      health = serviceHost.getHealth().toString();
      found = true;
    } catch (Exception ex) {
      logger.warning("init ServicesController: ".concat(ex.getMessage()));
    }

  }

  public String getHealth() {
    return health;
  }

  public String getService() {
    loadServices();
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    loadServices();
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getHostname() {
    loadServices();
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public boolean isFound() {
    return found;
  }

  public void setFound(boolean found) {
    this.found = found;
  }

  public List<InstanceInfo> getInstanceFullInfo() {
    loadServices();
    return instanceInfoList;
  }

  public void restartKagents(List<Hosts> hosts) {
    performKagentAction(hosts, KagentAction.RESTART);
  }
  
  public void startKagents(List<Hosts> hosts) {
    performKagentAction(hosts, KagentAction.START);
  }
  
  public void stopKagents(List<Hosts> hosts) {
    performKagentAction(hosts, KagentAction.STOP);
  }
  
  private void performKagentAction(List<Hosts> hosts, KagentAction action) {
    Map<Hosts, Future<RemoteCommandResult>> asyncResults = submitKagentAction(action, hosts);
    if (asyncResults != null) {
      boolean hadFailure = false;
      for (Map.Entry<Hosts, Future<RemoteCommandResult>> entry : asyncResults.entrySet()) {
        Future asyncResult = entry.getValue();
        try {
          if (asyncResult != null) {
            asyncResult.get();
          } else {
            throw new ExecutionException(new Throwable("Command failed"));
          }
        } catch (InterruptedException | ExecutionException ex) {
          FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Failed to " + action.name() + " agent",
              "Failed to " + action.name() + " agent@" + entry.getKey());
          FacesContext.getCurrentInstance().addMessage(null, message);
          hadFailure = true;
        }
      }
      if (!hadFailure) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "All agents have " + action.effect + " successfully",
            "");
        FacesContext.getCurrentInstance().addMessage(null, message);
      }
    }
  }
  
  private Map<Hosts, Future<RemoteCommandResult>> submitKagentAction(KagentAction action, List<Hosts> hosts) {
    if (hosts == null || hosts.isEmpty()) {
      FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "No hosts selected",
          "Hosts list is null");
      FacesContext.getCurrentInstance().addMessage(null, message);
      return null;
    }
    switch (action) {
      case START:
        return startAgentsInternal(hosts);
      case STOP:
        return stopAgentsInternal(hosts);
      case RESTART:
        return restartAgentsInternal(hosts);
      default:
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unknown action",
            "Unknown action to perform on kagent");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return null;
    }
  }
  
  private Map<Hosts, Future<RemoteCommandResult>> startAgentsInternal(List<Hosts> hosts) {
    Map<Hosts, Future<RemoteCommandResult>> asyncResults = new HashMap<>(hosts.size());
    for (Hosts host : hosts) {
      try {
        Future<RemoteCommandResult> asyncResult = agentLivenessMonitor.startAsync(host);
        asyncResults.put(host, asyncResult);
      } catch (ServiceException ex) {
        asyncResults.put(host, null);
      }
    }
    return asyncResults;
  }
  
  private Map<Hosts, Future<RemoteCommandResult>> stopAgentsInternal(List<Hosts> hosts) {
    Map<Hosts, Future<RemoteCommandResult>> asyncResults = new HashMap<>(hosts.size());
    for (Hosts host : hosts) {
      try {
        Future<RemoteCommandResult> asyncResult = agentLivenessMonitor.stopAsync(host);
        asyncResults.put(host, asyncResult);
      } catch (ServiceException ex) {
        asyncResults.put(host, null);
      }
    }
    return asyncResults;
  }
  
  private Map<Hosts, Future<RemoteCommandResult>> restartAgentsInternal(List<Hosts> hosts) {
    Map<Hosts, Future<RemoteCommandResult>> asyncResults = new HashMap<>(hosts.size());
    for (Hosts host : hosts) {
      try {
        Future<RemoteCommandResult> asyncResult = agentLivenessMonitor.restartAsync(host);
        asyncResults.put(host, asyncResult);
      } catch (ServiceException ex) {
        asyncResults.put(host, null);
      }
    }
    return asyncResults;
  }
  
  enum KagentAction {
    START("started"),
    STOP("stopped"),
    RESTART("restarted");
    
    private final String effect;
    
    KagentAction(String effect) {
      this.effect = effect;
    }
  }
}
