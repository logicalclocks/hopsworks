package se.kth.kthfsdashboard.host;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Stateless
public class HostEJB {

   @PersistenceContext(unitName = "kthfsPU")
   private EntityManager em;

   public HostEJB() {
   }

   public List<Host> findHosts() {
      TypedQuery<Host> query = em.createNamedQuery("findAllHosts", Host.class);
      return query.getResultList();
   }

   public Host findHostByName(String name) throws Exception {
      TypedQuery<Host> query = em.createNamedQuery("findHostByName", Host.class).setParameter("hostname", name);
      try {
         return query.getSingleResult();
      } catch (NoResultException ex) {
         throw new Exception("NoResultException");
      }
   }

   public Host findHostById(String id) throws Exception {
      TypedQuery<Host> query = em.createNamedQuery("findHostById", Host.class).setParameter("id", id);
      List<Host> result = query.getResultList();
      if (result.isEmpty()) {
          throw new Exception("NoResultException");
      } else if (result.size() == 1) {
         return result.get(0);
      } else {
         throw new Exception("MultipHostsFoundException");
      }
   }
   
   public boolean hostExists(String id) {
      TypedQuery<Host> query = em.createNamedQuery("findHostById", Host.class).setParameter("id", id);
      if (query.getResultList().isEmpty()){
         return false;
      }
      return true;
   }   

   public Host storeHost(Host host, boolean register) {
      if (register) {
         em.merge(host);
      } else {
         Host h = em.find(Host.class, host.getHostId());
         host.setPrivateIp(h.getPrivateIp());
         host.setPublicIp(h.getPublicIp());
         host.setCores(h.getCores());
         em.merge(host);
      }
      return host;
   }
}
