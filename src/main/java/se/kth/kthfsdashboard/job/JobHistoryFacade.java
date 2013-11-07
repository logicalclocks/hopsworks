/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.job;

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
public class JobHistoryFacade extends AbstractFacade<Job> {
    @PersistenceContext(unitName="kthfsPU")
    private EntityManager em;
    
     public JobHistoryFacade(){
        super(Job.class);
        
    }
     
     @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
     @Override
     public List<Job> findAll() {

        TypedQuery<Job> query = em.createNamedQuery("Job.findAll", Job.class);
        return query.getResultList();
    }
}
