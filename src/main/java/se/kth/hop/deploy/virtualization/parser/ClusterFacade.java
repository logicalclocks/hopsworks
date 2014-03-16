
package se.kth.hop.deploy.virtualization.parser;

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
public class ClusterFacade extends AbstractFacade<ClusterEntity> {
    
    @PersistenceContext(unitName="kthfsPU")
    private EntityManager em;
    
    public ClusterFacade(){
        super(ClusterEntity.class);
        
    }
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public void persistCluster(ClusterEntity cluster){
        em.persist(cluster);
    }
    
    public void removeCluster(ClusterEntity cluster){
        em.remove(em.merge(cluster));
    }
    
    @Override
     public List<ClusterEntity> findAll() {

        TypedQuery<ClusterEntity> query = em.createNamedQuery("ClustersEntity.findAll", ClusterEntity.class);
        return query.getResultList();
    }
    public void updateCluster(ClusterEntity cluster){
        em.merge(cluster);
    }
}
