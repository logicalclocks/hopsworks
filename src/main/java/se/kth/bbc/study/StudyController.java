/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
/**
 *
 * @author roshan
 */
@Stateless
public class StudyController {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }
        
    public StudyController() {
    }
      
    public List<TrackStudy> findAll() {
        TypedQuery<TrackStudy> query = em.createNamedQuery("TrackStudy.findAll", TrackStudy.class);
        return query.getResultList();
    }
    
    public List<TrackStudy> findByUser(String username){
        TypedQuery<TrackStudy> query = em.createNamedQuery("TrackStudy.findByUsername", TrackStudy.class).setParameter("username", username);
        return query.getResultList();
    }
    
    
    public long getAllStudy(){
        return (Long)em.createNamedQuery("TrackStudy.findAllStudy").getSingleResult();
    }
    
     public long getMembers(String name){
        return (Long)em.createNamedQuery("TrackStudy.findMembers").setParameter("name", name).getSingleResult();
    }
    
    public void persistStudy(TrackStudy study) {
        em.persist(study);
    }
    
    public void removeStudy(TrackStudy study) {
       em.remove(study);
    }
    
    public void addMember(StudyGroupMembers studyMember){
        em.persist(studyMember);
    }
    
    public void removeMember(StudyGroupMembers studyMember){
        em.remove(studyMember);
    }
}
