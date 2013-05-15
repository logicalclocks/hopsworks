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
    
    public List<Command> findRecentByClusterGroup(String cluster, String group) {

        TypedQuery<Command> query = 
                em.createNamedQuery("Commands.findRecentByCluster-Group", Command.class)
                .setParameter("cluster", cluster).setParameter("group", group)                
                .setParameter("status", Command.commandStatus.Running);
        return query.getResultList();
    }

    public List<Command> findRunningByClusterGroup(String cluster, String group) {

        TypedQuery<Command> query = 
                em.createNamedQuery("Commands.findRunningByCluster-Group", Command.class)
                .setParameter("cluster", cluster).setParameter("group", group)
                .setParameter("status", Command.commandStatus.Running);
        return query.getResultList();
    }    

    public void persistCommand(Command command) {
        em.persist(command);
    }

    public void updateCommand(Command command) {
        em.merge(command);
    }


}
