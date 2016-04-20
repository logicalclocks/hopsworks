/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class ExecutionInputfilesFacade extends AbstractFacade<ExecutionsInputfiles>{
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public ExecutionInputfilesFacade() {
        super(ExecutionsInputfiles.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public void create(int executionId, int inodePid, String name){
        ExecutionsInputfiles file = new ExecutionsInputfiles(executionId, inodePid, name);
        em.persist(file);
        em.flush();
    }
    
    public ExecutionsInputfiles findExecutionInputFileByExecutionId(int id){
        TypedQuery<ExecutionsInputfiles> q = em.createNamedQuery(
            "ExecutionsInputfiles.findByExecutionId", ExecutionsInputfiles.class);
        q.setParameter("id", id);
        return q.getSingleResult();
    }
    
    public List<ExecutionsInputfiles> findExecutionInputFileByInodePid(int parent_id){
        TypedQuery<ExecutionsInputfiles> q = em.createNamedQuery(
            "ExecutionsInputfiles.findByInodePid", ExecutionsInputfiles.class);
        q.setParameter("parent_id", parent_id);
        return q.getResultList();
    }
    
    public List<ExecutionsInputfiles> findExecutionInputFileByByInodeName(String inodeName){
        TypedQuery<ExecutionsInputfiles> q = em.createNamedQuery(
            "ExecutionsInputfiles.findByInodeName", ExecutionsInputfiles.class);
        q.setParameter("name", inodeName);
        return q.getResultList();
    }
    
}
