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

package io.hops.hopsworks.common.dao.kagent;

import io.hops.hopsworks.common.agent.AgentController;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.util.WebCommunication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.persistence.NonUniqueResultException;
import javax.ws.rs.core.Response;

@Stateless
public class HostServicesFacade {

  @EJB
  private WebCommunication web;
  @EJB
  private HostsFacade hostEJB;

  private static final Logger LOGGER = Logger.getLogger(HostServicesFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HostServicesFacade() {
  }

  public List<HostServices> findAll() {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findAll", HostServices.class);
    return query.getResultList();
  }

  public List<HostServices> findGroupServices(String group) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Group", HostServices.class).
        setParameter("group", group);
    return query.getResultList();
  }

  public List<HostServices> findGroups(String group, String service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Group-Service", HostServices.class)
        .setParameter("group", group).setParameter("service", service);
    return query.getResultList();
  }

  public List<String> findClusters() {
    TypedQuery<String> query = em.createNamedQuery("HostServices.findClusters",
        String.class);
    return query.getResultList();
  }

  public List<String> findGroups(String cluster) {
    TypedQuery<String> query = em.
        createNamedQuery("HostServices.findGroupsBy-Cluster", String.class)
        .setParameter("cluster", cluster);
    return query.getResultList();
  }

  public List<HostServices> findServiceOnHost(String hostname, String group, String service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findOnHost", HostServices.class)
        .setParameter("hostname", hostname).setParameter("group", group).setParameter("service", service);
    return query.getResultList();
  }

  public HostServices find(String hostname, String cluster, String group, String service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.find", HostServices.class)
        .setParameter("hostname", hostname).setParameter("cluster", cluster)
        .setParameter("group", group).setParameter("service", service);
    List results = query.getResultList();
    if (results.isEmpty()) {
      return null;
    } else if (results.size() == 1) {
      return (HostServices) results.get(0);
    }
    throw new NonUniqueResultException();
  }

  public List<HostServices> findHostServiceByHostname(String hostname) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.findBy-Hostname",
        HostServices.class)
        .setParameter("hostname", hostname);
    return query.getResultList();
  }

  public List<HostServices> findServices(String cluster, String group, String service) {
    TypedQuery<HostServices> query = em.createNamedQuery(
        "HostServices.findBy-Cluster-Group-Service", HostServices.class)
        .setParameter("cluster", cluster).setParameter("group", group).
        setParameter("service", service);
    return query.getResultList();
  }

  public List<HostServices> findServices(String service) {
    TypedQuery<HostServices> query = em.createNamedQuery(
        "HostServices.findBy-Service", HostServices.class)
        .setParameter("service", service);
    return query.getResultList();
  }

  public Long count(String cluster, String group, String service) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.Count", Long.class)
        .setParameter("cluster", cluster).setParameter("group", group)
        .setParameter("service", service);
    return query.getSingleResult();
  }

  public Long countHosts(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.Count-hosts", Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long countServices(String cluster, String group) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.Count-services", Long.class)
        .setParameter("cluster", cluster).setParameter("group", group);
    return query.getSingleResult();
  }

  public Long totalCores(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.TotalCores",
        Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long totalGPUs(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.TotalGPUs",
        Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long totalMemoryCapacity(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.TotalMemoryCapacity",
        Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long totalDiskCapacity(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("HostServices.TotalDiskCapacity",
        Long.class)
        .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public List<HostServicesInfo> findHostServicesByCluster(String cluster) {
    TypedQuery<HostServicesInfo> query = em.createNamedQuery(
        "HostServices.findHostServicesBy-Cluster", HostServicesInfo.class)
        .setParameter("cluster", cluster);
    return query.getResultList();
  }

  public List<HostServicesInfo> findHostServicesByGroup(String cluster, String group) {
    TypedQuery<HostServicesInfo> query = em.createNamedQuery(
        "HostServices.findHostServicesBy-Cluster-Group", HostServicesInfo.class)
        .setParameter("cluster", cluster).setParameter("group", group);
    return query.getResultList();
  }

  public List<HostServicesInfo> findHostServices(String cluster, String group,
      String service) {
    TypedQuery<HostServicesInfo> query = em.createNamedQuery(
        "HostServices.findHostServicesBy-Cluster-Group-Service", HostServicesInfo.class)
        .setParameter("cluster", cluster).setParameter("group", group)
        .setParameter("service", service);
    return query.getResultList();
  }
  
  public HostServicesInfo findHostServices(String cluster, String group, String service,
    String hostname) {
    TypedQuery<HostServicesInfo> query = em.
      createNamedQuery("HostServices.findHostServicesBy-Cluster-Group-Service-Host",
        HostServicesInfo.class)
      .setParameter("cluster", cluster).setParameter("group", group)
      .setParameter("service", service).setParameter("hostname", hostname);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  public String findCluster(String ip) {
    TypedQuery<String> query = em.createNamedQuery(
        "HostServices.find.ClusterBy-Ip.WebPort", String.class)
        .setParameter("ip", ip);
    return query.getSingleResult();
  }

  public String findPrivateIp(String cluster, String hostname) {
    TypedQuery<String> query = em.createNamedQuery(
        "HostServices.find.PrivateIpBy-Cluster.Hostname.WebPort", String.class)
        .setParameter("cluster", cluster).setParameter("hostname", hostname);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  public void persist(HostServices hostService) {
    em.persist(hostService);
  }

  public void store(HostServices service) {
    TypedQuery<HostServices> query = em.createNamedQuery("HostServices.find", HostServices.class)
        .setParameter("hostname", service.getHost().getHostname()).setParameter("cluster",
        service.getCluster())
        .setParameter("group", service.getGroup()).setParameter("service",
        service.getService());
    List<HostServices> s = query.getResultList();

    if (s.size() > 0) {
      service.setId(s.get(0).getId());
      em.merge(service);
    } else {
      em.persist(service);
    }
  }

  public void deleteServicesByHostname(String hostname) {
    em.createNamedQuery("HostServices.DeleteBy-Hostname").setParameter("hostname", hostname).executeUpdate();
  }

  public String serviceOp(String group, String serviceName, Action action) throws GenericException {
    return webOp(action, findGroups(group, serviceName));
  }

  public String serviceOp(String service, Action action) throws GenericException {
    return webOp(action, findGroupServices(service));
  }

  public String serviceOnHostOp(String group, String serviceName, String hostname,
      Action action) throws GenericException {
    return webOp(action, findServiceOnHost(hostname, group, serviceName));
  }

  private String webOp(Action operation, List<HostServices> services) throws GenericException {
    if (operation == null) {
      throw new IllegalArgumentException("The action is not valid, valid action are " + Arrays.toString(
              Action.values()));
    }
    if (services == null || services.isEmpty()) {
      throw new IllegalArgumentException("service was not provided.");
    }
    String result = "";
    boolean success = false;
    int exception = Response.Status.BAD_REQUEST.getStatusCode();
    for (HostServices service : services) {
      Hosts h = service.getHost();
      if (h != null) {
        String ip = h.getPublicOrPrivateIp();
        String agentPassword = h.getAgentPassword();
        try {
          result += service.toString() + " " + web.serviceOp(operation.value(), ip, agentPassword,
              service.getCluster(), service.getGroup(), service.getService());
          success = true;
        } catch (GenericException ex) {
          if (services.size() == 1) {
            throw ex;
          } else {
            exception = ex.getErrorCode().getRespStatus().getStatusCode();
            result += service.toString() + " " + ex.getErrorCode().getRespStatus() + " " + ex.getMessage();
          }
        }
      } else {
        result += service.toString() + " " + "host not found: " + service.getHost();
      }
      result += "\n";
    }
    if (!success) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE,
        "webOp error, exception: " + exception + ", " + "result: " + result);
    }
    return result;
  }

  private Hosts findHostById(String hostname) {
    Hosts host = hostEJB.findByHostname(hostname);
    return host;
  }

  public List<HostServices> updateHostServices(AgentController.AgentHeartbeatDTO heartbeat) throws ServiceException {
    Hosts host = hostEJB.findByHostname(heartbeat.getHostId());
    if (host == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_FOUND, Level.WARNING,
        "hostId: " + heartbeat.getHostId());
    }
    final List<HostServices> hostServices = new ArrayList<>(heartbeat.getServices().size());
    for (final AgentController.AgentServiceDTO service : heartbeat.getServices()) {
      final String cluster = service.getCluster();
      final String name = service.getService();
      final String group = service.getGroup();
      HostServices hostService = null;
      try {
        hostService = find(heartbeat.getHostId(), cluster, group, name);
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Could not find service for " + heartbeat.getHostId() + "/"
            + cluster + "/" + group + "/" + name);
        continue;
      }
      
      if (hostService == null) {
        hostService = new HostServices();
        hostService.setHost(host);
        hostService.setCluster(cluster);
        hostService.setGroup(group);
        hostService.setService(name);
        hostService.setStartTime(heartbeat.getAgentTime());
      }
  
      final Integer pid = service.getPid() != null ? service.getPid(): -1;
      hostService.setPid(pid);
      if (service.getStatus() != null) {
        if ((hostService.getStatus() == null || !hostService.getStatus().equals(Status.Started))
            && service.getStatus().equals(Status.Started)) {
          hostService.setStartTime(heartbeat.getAgentTime());
        }
        hostService.setStatus(service.getStatus());
      } else {
        hostService.setStatus(Status.None);
      }
  
      if (service.getStatus().equals(Status.Started)) {
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
}
