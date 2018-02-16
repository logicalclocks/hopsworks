/*
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
 *
 */

package io.hops.hopsworks.common.dao.host;

import java.io.Serializable;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.NonUniqueResultException;

@Stateless
public class HostsFacade implements Serializable {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public HostsFacade() {
  }

  public List<Hosts> find() {
    TypedQuery<Hosts> query = em.createNamedQuery("Hosts.find", Hosts.class);
    return query.getResultList();
  }

  public Hosts findByHostIp(String hostIp) throws Exception {
    TypedQuery<Hosts> query = em.createNamedQuery("Hosts.findBy-HostIp",
            Hosts.class).setParameter("hostIp", hostIp);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      throw new Exception("NoResultException");
    }
  }

  public Hosts findByHostname(String hostname) {
    TypedQuery<Hosts> query = em.createNamedQuery("Hosts.findBy-Hostname",
            Hosts.class).setParameter("hostname", hostname);
    List<Hosts> result = query.getResultList();
    if (result.isEmpty()) {
      return null;
    } else if (result.size() == 1) {
      return result.get(0);
    } else {
      throw new NonUniqueResultException(
              "Invalid program state - MultipleHostsFoundException. HostId should return a single host.");
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

  public boolean hostExists(String hostId) {
    try {
      if (findByHostname(hostId) != null) {
        return true;
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  public Hosts storeHost(Hosts host, boolean register) {
    if (register) {
      em.merge(host);
    } else {
      Hosts h = findByHostname(host.getHostname());
      host.setPrivateIp(h.getPrivateIp());
      host.setPublicIp(h.getPublicIp());
      host.setCores(h.getCores());
      host.setId(h.getId());
      em.merge(host);
    }
    return host;
  }
  
  public boolean removeByHostname(String hostname) {
    Hosts host = findByHostname(hostname);
    if (host != null) {
      em.remove(host);
      return true;
    }
    return false;
  }
}
