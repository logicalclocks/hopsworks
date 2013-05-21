package se.kth.kthfsdashboard.host;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
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

    public Host findHostByName(String name) {

        TypedQuery<Host> query = em.createNamedQuery("findHostByName", Host.class).setParameter("name", name);
        if (query.getResultList().size() > 0) {
            return query.getSingleResult();
        } else {
            return null;
        }
    }

    public Host storeHost(Host host, boolean register) {

        if (register) {
            em.merge(host);
        } else {
           Host h = em.find(Host.class, host.getHostname());
            host.setRack(h.getRack());
            host.setPrivateIp(h.getPrivateIp());
            host.setPublicIp(h.getPublicIp());
            host.setCores(h.getCores());
            em.merge(host);
        }
        return host;
    }

    public Host storeHostRackId(Host host) {

        this.em.merge(host);
        return host;
    }
}
