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

package io.hops.hopsworks.common.dao.host;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class HostsFacade extends AbstractFacade<Hosts> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HostsFacade() {
    super(Hosts.class);
  }
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public List<Hosts> findAllHosts() {
    TypedQuery<Hosts> query = em.createNamedQuery("Hosts.find", Hosts.class);
    return query.getResultList();
  }

  public Hosts findByHostIp(String hostIp) {
    TypedQuery<Hosts> query = em.createNamedQuery("Hosts.findBy-HostIp",
            Hosts.class).setParameter("hostIp", hostIp);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }
  
  public Hosts findByHostname(String hostname) {
    TypedQuery<Hosts> query = em.createNamedQuery("Hosts.findBy-Hostname",
      Hosts.class).setParameter("hostname", hostname);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  public List<Hosts> find(String cluster, String group, String service,
          Status status) {
    TypedQuery<Hosts> query = em.createNamedQuery(
            "Hosts.findBy-Cluster.Service.Role.Status", Hosts.class)
            .setParameter("cluster", cluster).setParameter("group", group)
            .setParameter("service", service).setParameter("status", status);
    return query.getResultList();
  }

  public List<Hosts> find(String cluster, String group, String service) {
    TypedQuery<Hosts> query = em.createNamedQuery(
            "Hosts.findBy-Cluster.Service.Role", Hosts.class)
            .setParameter("cluster", cluster).setParameter("group", group)
            .setParameter("service", service);
    return query.getResultList();
  }

  public String findCPUHost() {
    List<Hosts> hosts = findAllHosts();
    for(Hosts host: hosts){
      if(host.getNumGpus() == 0 && host.getCondaEnabled()){
        return host.getHostname();
      }
    }
    return null;
  }

  public String findGPUHost() {
    List<Hosts> hosts = findAllHosts();
    for(Hosts host: hosts){
      if(host.getNumGpus() > 0 && host.getCondaEnabled()){
        return host.getHostname();
      }
    }
    return null;
  }


  public boolean hostExists(String hostId) {
    try {
      return findByHostname(hostId) != null;
    } catch (Exception e) {
      return false;
    }
  }

  public Hosts storeHost(Hosts host) {
    return em.merge(host);
  }
  
  public boolean removeByHostname(String hostname) {
    Hosts host = findByHostname(hostname);
    if (host != null) {
      em.remove(host);
      return true;
    }
    return false;
  }

  public List<Hosts> getCondaHosts(LibraryFacade.MachineType machineType) {
    TypedQuery<Hosts> query;
    switch (machineType) {
      case CPU:
        query = em.createNamedQuery("Hosts.findBy-conda-enabled-cpu", Hosts.class);
        break;
      case GPU:
        query = em.createNamedQuery("Hosts.findBy-conda-enabled-gpu", Hosts.class);
        break;
      default:
        query = em.createNamedQuery("Hosts.findBy-conda-enabled", Hosts.class);
    }

    return query.getResultList();
  }
}
