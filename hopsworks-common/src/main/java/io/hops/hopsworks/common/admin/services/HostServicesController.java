/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.admin.services;

import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.ServiceStatus;
import io.hops.hopsworks.common.dao.kagent.Action;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.common.util.WebCommunication;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HostServicesController {
  
  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private WebCommunication web;
  @EJB
  private HostsController hostsController;
  
  private static final Logger LOGGER = Logger.getLogger(HostServicesController.class.getName());
  
  
  public HostServices find(Long serviceId) throws ServiceException {
    HostServices service = hostServicesFacade.find(serviceId);
    if (service == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.WARNING,
        "id: " + serviceId);
    }
    return service;
  }
  
  public HostServices findByName(String name, String hostname) throws ServiceException {
    Optional<HostServices> service = hostServicesFacade.findByServiceName(name, hostname);
    if (!service.isPresent()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.WARNING,
        "name: " + name);
    }
    return service.get();
  }
  
  public HostServices findByHostnameServiceNameGroup(String hostname, String group, String name)
    throws ServiceException {
    Optional<HostServices> service = hostServicesFacade.findByHostnameServiceNameGroup(hostname, group, name);
    if (!service.isPresent()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.WARNING,
        "name: " + name + ", hostname: " + hostname + ", group: " + group);
    }
    return service.get();
  }
  
  public void updateService(String hostname, String serviceName, Action action)
    throws ServiceException, GenericException {
    if (!Action.START_SERVICE.equals(action) && !Action.RESTART_SERVICE.equals(action) && !Action.STOP_SERVICE.equals
      (action)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ACTION_FORBIDDEN, Level.WARNING, "action: " + action);
    }
    HostServices service = findByName(serviceName, hostname);
    webOp(action, Collections.singletonList(service));
  }
  
  public String groupOp(String group, Action action) throws GenericException {
    return webOp(action, hostServicesFacade.findGroupServices(group));
  }
  
  public String serviceOp(String service, Action action) throws GenericException {
    return webOp(action, hostServicesFacade.findServices(service));
  }
  
  public String serviceOnHostOp(String group, String serviceName, String hostname,
    Action action) throws GenericException, ServiceException {
    return webOp(action, Collections.singletonList(findByHostnameServiceNameGroup(hostname, group, serviceName)));
  }
  
  private String webOp(Action operation, List<HostServices> services) throws GenericException {
    if (operation == null) {
      throw new IllegalArgumentException("The action is not valid, valid action are " + Arrays.toString(
        Action.values()));
    }
    if (services == null || services.isEmpty()) {
      throw new IllegalArgumentException("service was not provided.");
    }
    StringBuilder result = new StringBuilder();
    boolean success = false;
    int exception = Response.Status.BAD_REQUEST.getStatusCode();
    for (HostServices service : services) {
      Hosts h = service.getHost();
      if (h != null) {
        String ip = h.getPublicOrPrivateIp();
        String agentPassword = h.getAgentPassword();
        try {
          result.append(service.toString()).append(" ").append(web.serviceOp(operation.value(), ip, agentPassword,
            service.getGroup(), service.getName()));
          success = true;
        } catch (GenericException ex) {
          if (services.size() == 1) {
            throw ex;
          } else {
            exception = ex.getErrorCode().getRespStatus().getStatusCode();
            result.append(service.toString()).append(" ").append(ex.getErrorCode().getRespStatus()).append(" ")
              .append(ex.getMessage());
          }
        }
      } else {
        result.append(service.toString()).append(" ").append("host not found: ").append(service.getHost());
      }
      result.append("\n");
    }
    if (!success) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE,
        "webOp error, exception: " + exception + ", " + "result: " + result.toString());
    }
    return result.toString();
  }
  
  public List<HostServices> updateHostServices(AgentController.AgentHeartbeatDTO heartbeat) throws ServiceException {
//    for (final AgentController.AgentServiceDTO service : heartbeat.getServices()) {
//      LOGGER.log(Level.FINE, "!!! hostname:" + heartbeat.getHostId() + "/" + service.getName() + "/" +
//        service.getGroup() + "/" + service.getStatus());
//    }
    Hosts host = hostsController.findByHostname(heartbeat.getHostId());
    final List<HostServices> hostServices = new ArrayList<>(heartbeat.getServices().size());
    for (final AgentController.AgentServiceDTO service : heartbeat.getServices()) {
      final String name = service.getName();
      final String group = service.getGroup();
      HostServices hostService;
      try {
        hostService = hostServicesFacade.findByHostnameServiceNameGroup(heartbeat.getHostId(), group, name)
          .orElse(null);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Could not find service for " + heartbeat.getHostId() + "/" + group + "/" + name);
        continue;
      }
      
      if (hostService == null) {
        hostService = new HostServices();
        hostService.setHost(host);
        hostService.setGroup(group);
        hostService.setName(name);
        hostService.setStartTime(heartbeat.getAgentTime());
      }
      
      final Integer pid = service.getPid() != null ? service.getPid(): -1;
      hostService.setPid(pid);
      if (service.getStatus() != null) {
        if ((hostService.getStatus() == null || hostService.getStatus() != ServiceStatus.Started)
          && service.getStatus() == ServiceStatus.Started) {
          hostService.setStartTime(heartbeat.getAgentTime());
        }
        hostService.setStatus(service.getStatus());
      } else {
        hostService.setStatus(ServiceStatus.None);
      }
      
      if (service.getStatus() == ServiceStatus.Started) {
        hostService.setStopTime(heartbeat.getAgentTime());
      }
      final Long startTime = hostService.getStartTime();
      final Long stopTime = hostService.getStopTime();
      if (startTime != null && stopTime != null) {
        hostService.setUptime(stopTime - startTime);
      } else {
        hostService.setUptime(0L);
      }
      
      store(hostService);
      hostServices.add(hostService);
    }
    return hostServices;
  }
  
  public void store(HostServices service) {
    Optional<HostServices> s = hostServicesFacade.findByHostnameServiceNameGroup(
      service.getHost().getHostname(),
      service.getGroup(),
      service.getName());
    
    if (s.isPresent()) {
      service.setId(s.get().getId());
      hostServicesFacade.update(service);
    } else {
      hostServicesFacade.save(service);
    }
  }
}
