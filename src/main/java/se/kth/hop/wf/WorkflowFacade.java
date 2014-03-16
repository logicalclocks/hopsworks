/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.wf;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.hop.wf.job.Job;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Stateless
public class WorkflowFacade extends AbstractFacade<Workflow> {
    @PersistenceContext(unitName="kthfsPU")
    private EntityManager em;
    
    public WorkflowFacade(){
        super(Workflow.class);
        
    }
     
     @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
     @Override
     public List<Workflow> findAll() {

        TypedQuery<Workflow> query = em.createNamedQuery("Workflow.findAll", Workflow.class);
        return query.getResultList();
    }

    @Override
    public void create(Workflow entity) {
        super.create(entity);
    }

    @Override
    public void edit(Workflow entity) {
        super.edit(entity);
    }

    @Override
    public void remove(Workflow entity) {
        super.remove(entity);
    }
}
