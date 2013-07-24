/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

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
public class DeploymentProgressFacade extends AbstractFacade<NodeProgression> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public DeploymentProgressFacade() {
        super(NodeProgression.class);

    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<NodeProgression> findAll() {
        TypedQuery<NodeProgression> query = em.createNamedQuery("NodeProgression.findAll", NodeProgression.class);
        return query.getResultList();
    }

    public void persistNodeProgress(NodeProgression progress) {
        em.persist(progress);
    }

    public void removeNodeProgress(NodeProgression progress) {
        em.remove(em.merge(progress));
    }
    
    public void updateProgress(NodeProgression progress){
        em.merge(progress);
    }
}
