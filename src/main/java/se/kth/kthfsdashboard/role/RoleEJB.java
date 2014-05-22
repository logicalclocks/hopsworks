package se.kth.kthfsdashboard.role;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.struct.RoleHostInfo;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Stateless
public class RoleEJB {

   @PersistenceContext(unitName = "kthfsPU")
   private EntityManager em;

   public RoleEJB() {
   }

   public List<String> findClusters() {
      TypedQuery<String> query = em.createNamedQuery("Role.findClusters", String.class);
      return query.getResultList();
   }

   public List<String> findServices(String cluster) {
      TypedQuery<String> query = em.createNamedQuery("Role.findServicesBy-Cluster", String.class)
              .setParameter("cluster", cluster);
      return query.getResultList();
   }
   
   public List<String> findServices() {
      TypedQuery<String> query = em.createNamedQuery("Role.findServices", String.class);
      return query.getResultList();
   }   

   public Role find(String hostId, String cluster, String service, String role) throws Exception{
      TypedQuery<Role> query = em.createNamedQuery("Role.find", Role.class)
              .setParameter("hostId", hostId).setParameter("cluster", cluster)
              .setParameter("service", service).setParameter("role", role);
      try {
         return query.getSingleResult();
      } catch (NoResultException ex) {
         throw new Exception("NoResultException");
      }
   }
   
   public List<Role> findHostRoles(String hostId) {
      TypedQuery<Role> query = em.createNamedQuery("Role.findBy-HostId", Role.class)
              .setParameter("hostId", hostId);
      return query.getResultList();
   }   

   public List<Role> findRoles(String cluster, String service, String role) {
      TypedQuery<Role> query = em.createNamedQuery("Role.findBy-Cluster-Service-Role", Role.class)
              .setParameter("cluster", cluster).setParameter("service", service).setParameter("role", role);
      return query.getResultList();
   }

   public Long count(String cluster, String service, String role) {
      TypedQuery<Long> query = em.createNamedQuery("Role.Count", Long.class)
              .setParameter("cluster", cluster).setParameter("service", service)
              .setParameter("role", role);
      return query.getSingleResult();
   }
   
   public Long countHosts(String cluster) {
      TypedQuery<Long> query = em.createNamedQuery("Role.Count-hosts", Long.class)
              .setParameter("cluster", cluster);
      return query.getSingleResult();
   }   
   
   public Long countRoles(String cluster, String service) {
      TypedQuery<Long> query = em.createNamedQuery("Role.Count-roles", Long.class)
              .setParameter("cluster", cluster).setParameter("service", service);
      return query.getSingleResult();
   }   
   
   public Long totalCores(String cluster) {
      TypedQuery<Long> query = em.createNamedQuery("RoleHost.TotalCores", Long.class)
              .setParameter("cluster", cluster);
      return query.getSingleResult();
   }    
   
   public Long totalMemoryCapacity(String cluster) {
      TypedQuery<Long> query = em.createNamedQuery("RoleHost.TotalMemoryCapacity", Long.class)
              .setParameter("cluster", cluster);
      return query.getSingleResult();
   }
   
   public Long totalDiskCapacity(String cluster) {
      TypedQuery<Long> query = em.createNamedQuery("RoleHost.TotalDiskCapacity", Long.class)
              .setParameter("cluster", cluster);
      return query.getSingleResult();
   }      

   public List<RoleHostInfo> findRoleHost(String cluster) {
      TypedQuery<RoleHostInfo> query = em.createNamedQuery("Role.findRoleHostBy-Cluster", RoleHostInfo.class)
              .setParameter("cluster", cluster);
      return query.getResultList();
   }
   public List<RoleHostInfo> findRoleHost(String cluster, String service) {
      TypedQuery<RoleHostInfo> query = em.createNamedQuery("Role.findRoleHostBy-Cluster-Service", RoleHostInfo.class)
              .setParameter("cluster", cluster).setParameter("service", service);
      return query.getResultList();
   }   
   
   public List<RoleHostInfo> findRoleHost(String cluster, String service, String role) {
      TypedQuery<RoleHostInfo> query = em.createNamedQuery("Role.findRoleHostBy-Cluster-Service-Role", RoleHostInfo.class)
              .setParameter("cluster", cluster).setParameter("service", service)
              .setParameter("role", role);
      return query.getResultList();
   } 
   
   public RoleHostInfo findRoleHost(String cluster, String service, String role, String hostId) throws Exception{
      TypedQuery<RoleHostInfo> query = em.createNamedQuery("Role.findRoleHostBy-Cluster-Service-Role-Host", RoleHostInfo.class)
              .setParameter("cluster", cluster).setParameter("service", service)
              .setParameter("role", role).setParameter("hostid", hostId);
      try {
         return query.getSingleResult();
      } catch (NoResultException ex) {
         throw new Exception("NoResultException");
      }
   }          
   
   public String findCluster(String ip, int webPort) {
      TypedQuery<String> query = em.createNamedQuery("RoleHost.find.ClusterBy-Ip.WebPort", String.class)
              .setParameter("ip", ip).setParameter("webPort", webPort);
      return query.getSingleResult();
   }   
   
   public String findPrivateIp(String cluster, String hostname, int webPort) {
      TypedQuery<String> query = em.createNamedQuery("RoleHost.find.PrivateIpBy-Cluster.Hostname.WebPort", String.class)
              .setParameter("cluster", cluster).setParameter("hostname", hostname)
              .setParameter("webPort", webPort);
      try {
         return query.getSingleResult();
      } catch (NoResultException ex) {
         return null;
      }
   }
   
   public void persist(Role role) {
      em.persist(role);
   }

   public void store(Role role) {
      TypedQuery<Role> query = em.createNamedQuery("Role.find", Role.class)
              .setParameter("hostId", role.getHostId()).setParameter("cluster", role.getCluster())
              .setParameter("service", role.getService()).setParameter("role", role.getRole());
      List<Role> s = query.getResultList();

      if (s.size() > 0) {
         role.setId(s.get(0).getId());
         em.merge(role);
      } else {
         em.persist(role);
      }
   }
   
   public void deleteRolesByHostId(String hostId) {
       em.createNamedQuery("Role.DeleteBy-HostId").setParameter("hostId", hostId).executeUpdate();
   }

   
   public boolean supportsInit(String cluster, String service, String role) {
      TypedQuery<Boolean> query = 
              em.createNamedQuery("Role.supportsInitBy-Cluster-Service-Role", Boolean.class)
              .setParameter("cluster", cluster).setParameter("service", service)
              .setParameter("role", role);
      return query.getSingleResult();
   }   
}