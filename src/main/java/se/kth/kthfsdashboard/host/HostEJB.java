package se.kth.kthfsdashboard.host;

import java.io.Serializable;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.struct.Status;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Stateless
public class HostEJB implements Serializable{

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public HostEJB() {
    }

    public List<Host> find() {
        TypedQuery<Host> query = em.createNamedQuery("Host.find", Host.class);
        return query.getResultList();
    }

    public Host findByHostname(String hostname) throws Exception {
        TypedQuery<Host> query = em.createNamedQuery("Host.findBy-Hostname", Host.class).setParameter("hostname", hostname);
        try {
            return query.getSingleResult();
        } catch (NoResultException ex) {
            throw new Exception("NoResultException");
        }
    }

    public Host findByHostId(String hostId) throws Exception {
        TypedQuery<Host> query = em.createNamedQuery("Host.findBy-HostId", Host.class).setParameter("hostId", hostId);
        List<Host> result = query.getResultList();
        if (result.isEmpty()) {
            return null;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            throw new Exception("MultipHostsFoundException");
        }
    }
    
    public List<Host> find(String cluster, String service, String role, Status status) {
        TypedQuery<Host> query = em.createNamedQuery("Host.findBy-Cluster.Service.Role.Status", Host.class)
                .setParameter("cluster", cluster).setParameter("service", service)
                .setParameter("role", role).setParameter("status", status);
        return query.getResultList();
    }    
    
    public List<Host> find(String cluster, String service, String role) {
        TypedQuery<Host> query = em.createNamedQuery("Host.findBy-Cluster.Service.Role", Host.class)
                .setParameter("cluster", cluster).setParameter("service", service)
                .setParameter("role", role);
        return query.getResultList();
    }     

    public boolean hostExists(String hostId) {
        try {
            if (findByHostId(hostId) != null) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
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
