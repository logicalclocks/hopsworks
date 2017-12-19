package io.hops.hopsworks.common.dao.role;

import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.WebCommunication;
import java.util.Arrays;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.persistence.NonUniqueResultException;
import javax.ws.rs.core.Response;

@Stateless
public class RoleEJB {

  @EJB
  private WebCommunication web;
  @EJB
  private HostEJB hostEJB;

  final static Logger logger = Logger.getLogger(RoleEJB.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public RoleEJB() {
  }

  public List<Roles> findAll() {
    TypedQuery<Roles> query = em.createNamedQuery("Roles.findAll", Roles.class);
    return query.getResultList();
  }

  public List<Roles> findServiceRoles(String serviceName) {
    TypedQuery<Roles> query = em.createNamedQuery("Roles.findBy-Service", Roles.class).
        setParameter("service", serviceName);
    return query.getResultList();
  }

  public List<Roles> findRoles(String service, String role) {
    TypedQuery<Roles> query = em.createNamedQuery("Roles.findBy-Service-Role", Roles.class)
        .setParameter("service", service).setParameter("role", role);
    return query.getResultList();
  }

  public List<String> findClusters() {
    TypedQuery<String> query = em.createNamedQuery("Roles.findClusters",
            String.class);
    return query.getResultList();
  }

  public List<String> findServices(String cluster) {
    TypedQuery<String> query = em.
            createNamedQuery("Roles.findServicesBy-Cluster", String.class)
            .setParameter("cluster", cluster);
    return query.getResultList();
  }

  public List<String> findServices() {
    TypedQuery<String> query = em.createNamedQuery("Roles.findServices",
            String.class);
    return query.getResultList();
  }

  public List<Roles> findRoleOnHost(String hostname, String service, String role) {

    TypedQuery<Roles> query = em.createNamedQuery("Roles.findOnHost", Roles.class)
        .setParameter("hostname", hostname).setParameter("service", service).setParameter("role", role);
    return query.getResultList();
  }

  public Roles find(String hostname, String cluster, String service, String role) {

    TypedQuery<Roles> query = em.createNamedQuery("Roles.find", Roles.class)
            .setParameter("hostname", hostname).setParameter("cluster", cluster)
            .setParameter("service", service).setParameter("role", role);
    List results = query.getResultList();
    if (results.isEmpty()) {
      return null;
    } else if (results.size() == 1) {
      return (Roles) results.get(0);
    }
    throw new NonUniqueResultException();
  }

  public List<Roles> findHostRoles(String hostname) {
    TypedQuery<Roles> query = em.createNamedQuery("Roles.findBy-HostId",
            Roles.class)
            .setParameter("hostname", hostname);
    return query.getResultList();
  }

  public List<Roles> findRoles(String cluster, String service, String role) {
    TypedQuery<Roles> query = em.createNamedQuery(
            "Roles.findBy-Cluster-Service-Role", Roles.class)
            .setParameter("cluster", cluster).setParameter("service", service).
            setParameter("role", role);
    return query.getResultList();
  }

  public List<Roles> findRoles(String role) {
    TypedQuery<Roles> query = em.createNamedQuery(
        "Roles.findBy-Role", Roles.class)
        .setParameter("role", role);
    return query.getResultList();
  }
  
  public Long count(String cluster, String service, String role) {
    TypedQuery<Long> query = em.createNamedQuery("Roles.Count", Long.class)
            .setParameter("cluster", cluster).setParameter("service", service)
            .setParameter("role", role);
    return query.getSingleResult();
  }

  public Long countHosts(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("Roles.Count-hosts", Long.class)
            .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long countRoles(String cluster, String service) {
    TypedQuery<Long> query = em.createNamedQuery("Roles.Count-roles", Long.class)
            .setParameter("cluster", cluster).setParameter("service", service);
    return query.getSingleResult();
  }

  public Long totalCores(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("RoleHost.TotalCores",
            Long.class)
            .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long totalMemoryCapacity(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("RoleHost.TotalMemoryCapacity",
            Long.class)
            .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public Long totalDiskCapacity(String cluster) {
    TypedQuery<Long> query = em.createNamedQuery("RoleHost.TotalDiskCapacity",
            Long.class)
            .setParameter("cluster", cluster);
    return query.getSingleResult();
  }

  public List<RoleHostInfo> findRoleHost(String cluster) {
    TypedQuery<RoleHostInfo> query = em.createNamedQuery(
            "Roles.findRoleHostBy-Cluster", RoleHostInfo.class)
            .setParameter("cluster", cluster);
    return query.getResultList();
  }

  public List<RoleHostInfo> findRoleHost(String cluster, String service) {
    TypedQuery<RoleHostInfo> query = em.createNamedQuery(
            "Roles.findRoleHostBy-Cluster-Service", RoleHostInfo.class)
            .setParameter("cluster", cluster).setParameter("service", service);
    return query.getResultList();
  }

  public List<RoleHostInfo> findRoleHost(String cluster, String service,
          String role) {
    TypedQuery<RoleHostInfo> query = em.createNamedQuery(
            "Roles.findRoleHostBy-Cluster-Service-Role", RoleHostInfo.class)
            .setParameter("cluster", cluster).setParameter("service", service)
            .setParameter("role", role);
    return query.getResultList();
  }

  public RoleHostInfo findRoleHost(String cluster, String service, String role,
          String hostname) throws Exception {
    TypedQuery<RoleHostInfo> query = em.createNamedQuery(
            "Roles.findRoleHostBy-Cluster-Service-Role-Host", RoleHostInfo.class)
            .setParameter("cluster", cluster).setParameter("service", service)
            .setParameter("role", role).setParameter("hostname", hostname);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      throw new Exception("NoResultException");
    }
  }

  public String findCluster(String ip, int webPort) {
    TypedQuery<String> query = em.createNamedQuery(
            "RoleHost.find.ClusterBy-Ip.WebPort", String.class)
            .setParameter("ip", ip);
    return query.getSingleResult();
  }

  public String findPrivateIp(String cluster, String hostname, int webPort) {
    TypedQuery<String> query = em.createNamedQuery(
            "RoleHost.find.PrivateIpBy-Cluster.Hostname.WebPort", String.class)
            .setParameter("cluster", cluster).setParameter("hostname", hostname);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  public void persist(Roles role) {
    em.persist(role);
  }

  public void store(Roles role) {
    TypedQuery<Roles> query = em.createNamedQuery("Roles.find", Roles.class)
            .setParameter("hostname", role.getHost().getHostname()).setParameter("cluster",
            role.getCluster())
            .setParameter("service", role.getService()).setParameter("role",
            role.getRole());
    List<Roles> s = query.getResultList();

    if (s.size() > 0) {
      role.setId(s.get(0).getId());
      em.merge(role);
    } else {
      em.persist(role);
    }
  }

  public void deleteRolesByHostname(String hostname) {
    em.createNamedQuery("Roles.DeleteBy-HostId").setParameter("hostname", hostname).
            executeUpdate();
  }

  public String roleOp(String service, String roleName, Action action) throws AppException {
    return webOp(action, findRoles(service, roleName));
  }

  public String serviceOp(String service, Action action) throws AppException {
    return webOp(action, findServiceRoles(service));
  }

  public String roleOnHostOp(String service, String roleName, String hostname,
          Action action) throws AppException {
    return webOp(action, findRoleOnHost(hostname, service, roleName));
  }

  private String webOp(Action operation, List<Roles> roles) throws AppException {
    if (operation == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "The action is not valid, valid action are " + Arrays.toString(
                      Action.values()));
    }
    if (roles == null || roles.isEmpty()) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "role not found");
    }
    String result = "";
    boolean success = false;
    int exception = Response.Status.BAD_REQUEST.getStatusCode();
    for (Roles role : roles) {
      Hosts h = role.getHost();
      if (h != null) {
        String ip = h.getPublicOrPrivateIp();
        String agentPassword = h.getAgentPassword();
        try {
          result += role.toString() + " " + web.roleOp(operation.value(), ip, agentPassword,
              role.getCluster(), role.getService(), role.getRole());
          success = true;
        } catch (AppException ex) {
          if (roles.size() == 1) {
            throw ex;
          } else {
            exception = ex.getStatus();
            result += role.toString() + " " + ex.getStatus() + " " + ex.getMessage();
          }
        }
      } else {
        result += role.toString() + " " + "host not found: " + role.getHost();
      }
      result += "\n";
    }
    if (!success) {
      throw new AppException(exception, result);
    }
    return result;
  }

  private Hosts findHostById(String hostname) {
    Hosts host = hostEJB.findByHostname(hostname);
    return host;
  }

}
