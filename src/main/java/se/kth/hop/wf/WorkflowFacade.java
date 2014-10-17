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
@Stateless (name = "oldFacade")
public class WorkflowFacade extends AbstractFacade<WorkflowOld> {
    @PersistenceContext(unitName="kthfsPU")
    private EntityManager em;
    
    public WorkflowFacade(){
        super(WorkflowOld.class);
        
    }
     
     @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
     @Override
     public List<WorkflowOld> findAll() {

        TypedQuery<WorkflowOld> query = em.createNamedQuery("WorkflowOld.findAll", WorkflowOld.class);
        return query.getResultList();
    }

    @Override
    public void create(WorkflowOld entity) {
        super.create(entity);
    }

    @Override
    public void edit(WorkflowOld entity) {
        super.edit(entity);
    }

    @Override
    public void remove(WorkflowOld entity) {
        super.remove(entity);
    }
}
