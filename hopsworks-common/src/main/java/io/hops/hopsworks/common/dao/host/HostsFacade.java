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
