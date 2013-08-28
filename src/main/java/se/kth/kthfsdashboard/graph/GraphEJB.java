package se.kth.kthfsdashboard.graph;

import java.io.Serializable;
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
public class GraphEJB  implements Serializable{

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public GraphEJB() {
    }
    
    public void importGraphs(List<Graph> graphs) {
        em.createNamedQuery("Graphs.removeAll").executeUpdate();
        for (Graph graph : graphs) {
            em.persist(graph);
        }
    }
    
    public void updateGraphs(String target, List<Graph> graphs, List<Graph> selectedGraphs) {
        for (Graph graph : graphs) {
            if (selectedGraphs.contains(graph)) {
                graph.setSelected(true);
            } else {
                graph.setSelected(false); 
            }
            em.merge(graph);
        }
    }

    public List<Graph> find(String target) {
        TypedQuery<Graph> query = em.createNamedQuery("Graphs.find-By.Target", Graph.class)
                .setParameter("target", target);
        return query.getResultList();
    }    

        public List<String> findTargets() {
        TypedQuery<String> query = em.createNamedQuery("Graphs.find.Targets", String.class);
        return query.getResultList();
    }   

    public Graph find(String target, String graphId) {
        TypedQuery<Graph> query = em.createNamedQuery("Graphs.find", Graph.class)
                .setParameter("graphId", graphId).setParameter("target", target);
        return query.getSingleResult();
    }
    
    public List<String> findIds(String target) {
        TypedQuery<String> query = em.createNamedQuery("Graphs.find.Ids-By.Target", String.class)
                .setParameter("target", target);
        return query.getResultList();
    }  
    
    public List<String> findSelectedIds(String target, String group) {
        TypedQuery<String> query = em.createNamedQuery("Graphs.find.SelectedIds-By.Target.Group", String.class)
                .setParameter("target", target).setParameter("group", group);
        return query.getResultList();
    }
    
    public List<String> findGroups(String target) {
        TypedQuery<String> query = em.createNamedQuery("Graphs.find.Groups-By.Target", String.class)
                .setParameter("target", target);
        return query.getResultList();
    }     
}
