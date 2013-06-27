package se.kth.kthfsdashboard.role;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.struct.StatusCount;

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

   public Long clusterMachinesCount(String cluster) {
      TypedQuery<Long> query = em.createNamedQuery("Role.Count-hosts", Long.class)
              .setParameter("cluster", cluster);
      return query.getSingleResult();
   }

   public List<String> findClusters() {
      TypedQuery<String> query = em.createNamedQuery("Role.findClusters", String.class);
      return query.getResultList();
   }

   public List<String> findServices(String cluster) {
      TypedQuery<String> query = em.createNamedQuery("Role.findServices", String.class)
              .setParameter("cluster", cluster);
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

   public List<Role> findRoles(String cluster) {
      TypedQuery<Role> query = em.createNamedQuery("Role.findBy.Cluster", Role.class)
              .setParameter("cluster", cluster);
      return query.getResultList();
   }

   public List<Role> findRoles(String cluster, String service) {
      TypedQuery<Role> query = em.createNamedQuery("Role.findBy-Cluster-Group", Role.class)
              .setParameter("cluster", cluster).setParameter("service", service);
      return query.getResultList();
   }

   public List<Role> findRoles(String cluster, String service, String role) {
      TypedQuery<Role> query = em.createNamedQuery("Role.findBy-Cluster-Group-Role", Role.class)
              .setParameter("cluster", cluster).setParameter("service", service).setParameter("role", role);
      return query.getResultList();
   }
   
   public Role findOneRole(String cluster, String service, String role) {
      TypedQuery<Role> query = em.createNamedQuery("Role.findBy-Cluster-Group-Role", Role.class)
              .setParameter("cluster", cluster).setParameter("service", service).setParameter("role", role);
      return query.getSingleResult();
   }   

   public List<Role> findRoles(String cluster, String service, String role, Status status) {
      TypedQuery<Role> query = em.createNamedQuery("Role.findBy-Cluster-Group-Role-Status", Role.class)
              .setParameter("cluster", cluster).setParameter("service", service).setParameter("role", role)
              .setParameter("status", status);
      return query.getResultList();
   }

   public List<String> findHostId(String cluster, String service, String role) {
      TypedQuery<String> query = em.createNamedQuery("Role.findHostIdBy-Cluster-Group-Role", String.class)
              .setParameter("cluster", cluster).setParameter("service", service).setParameter("role", role);
      return query.getResultList();
   }

   public Long count(String cluster, String service, String role) {
      TypedQuery<Long> query = em.createNamedQuery("Role.Count", Long.class)
              .setParameter("cluster", cluster).setParameter("service", service)
              .setParameter("role", role);
      return query.getSingleResult();
   }

   public Long countStatus(String cluster, String service, String role, Status status) {
      TypedQuery<Long> query = em.createNamedQuery("Role.CountBy-Cluster-Service-Role-Status", Long.class)
              .setParameter("cluster", cluster).setParameter("service", service)
              .setParameter("role", role).setParameter("status", status);
      return query.getSingleResult();
   }
   
   public List<StatusCount> countStatus(String cluster, String service, String role) {
      TypedQuery<StatusCount> query = em.createNamedQuery("Role.findStatusCountBy-Cluster-Group-Role", StatusCount.class)
              .setParameter("cluster", cluster).setParameter("service", service)
              .setParameter("role", role);
      return query.getResultList();
   }   

   public void persist(Role role) {
      em.persist(role);
   }

   public void store(Role role) {
      TypedQuery<Role> query = em.createNamedQuery("Role.findBy-Cluster-Service-Role-HostId", Role.class)
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

}