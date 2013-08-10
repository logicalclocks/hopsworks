package se.kth.kthfsdashboard.command;

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
public class CommandEJB {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public CommandEJB() {
    }

    public List<Command> findAll() {

        TypedQuery<Command> query = em.createNamedQuery("Commands.find", Command.class);
        return query.getResultList();
    }
    
    public List<Command> findRecentByCluster(String cluster) {

        TypedQuery<Command> query = 
                em.createNamedQuery("Commands.findRecentByCluster", Command.class)
                .setParameter("cluster", cluster)                
                .setParameter("status", Command.commandStatus.Running);;
        return query.getResultList();
    }

    public List<Command> findRunningByCluster(String cluster) {

        TypedQuery<Command> query = 
                em.createNamedQuery("Commands.findRunningByCluster", Command.class)
                .setParameter("cluster", cluster)
                .setParameter("status", Command.commandStatus.Running);
        return query.getResultList();
    }
    
    public List<Command> findRecentByClusterService(String cluster, String service) {

        TypedQuery<Command> query = 
                em.createNamedQuery("Commands.findRecentByCluster-Service", Command.class)
                .setParameter("cluster", cluster).setParameter("service", service)                
                .setParameter("status", Command.commandStatus.Running);
        return query.getResultList();
    }

    public List<Command> findRunningByClusterService(String cluster, String service) {

        TypedQuery<Command> query = 
                em.createNamedQuery("Commands.findRunningByCluster-Service", Command.class)
                .setParameter("cluster", cluster).setParameter("service", service)
                .setParameter("status", Command.commandStatus.Running);
        return query.getResultList();
    }    
    
    public List<Command> findRecentByClusterServiceRoleHostId(String cluster, String service, String role, String hostId) {

        TypedQuery<Command> query = 
                em.createNamedQuery("Commands.findRecentByCluster-Service-Role-HostId", Command.class)
                .setParameter("cluster", cluster).setParameter("service", service)
                .setParameter("role", role).setParameter("hostId", hostId)                
                .setParameter("status", Command.commandStatus.Running);
        return query.getResultList();
    }
    
    public List<Command> findLatestByClusterServiceRoleHostId(String cluster, String service, String role, String hostId) {

        TypedQuery<Command> query = 
                em.createNamedQuery("Commands.findByCluster-Service-Role-HostId", Command.class)
                .setParameter("cluster", cluster).setParameter("service", service)
                .setParameter("role", role).setParameter("hostId", hostId);
        return query.setMaxResults(1).getResultList();
    }    
    
    public void persistCommand(Command command) {
        em.persist(command);
    }

    public void updateCommand(Command command) {
        em.merge(command);
    }


}
