
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;


/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Stateless
public class ClusterFacade extends AbstractFacade<Cluster> {
    
    @PersistenceContext(unitName="kthfsPU")
    private EntityManager em;
    
    public ClusterFacade(){
        super(Cluster.class);
        
    }
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public void persistCluster(Cluster cluster){
        em.persist(cluster);
    }
    
    public void removeCluster(Cluster cluster){
        em.remove(em.merge(cluster));
    }
    
    @Override
     public List<Cluster> findAll() {

        TypedQuery<Cluster> query = em.createNamedQuery("Clusters.findAll", Cluster.class);
        return query.getResultList();
    }
}
