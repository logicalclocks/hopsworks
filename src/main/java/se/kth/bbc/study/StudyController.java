/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.time.DateUtils;

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
    
    
    public long getAllStudy(String username){
        return (Long)em.createNamedQuery("TrackStudy.findAllStudy").setParameter("username", username).getSingleResult();
    }
    
    public long getMembers(String name){
        return (Long)em.createNamedQuery("TrackStudy.findMembers").setParameter("name", name).getSingleResult();
    }
    
    public List<TrackStudy> filterLatestStudy(String username){
    
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(new Date());
//        cal.add(Calendar.DATE, -2);
//        
//        Query query = 
//                em.createQuery("SELECT t FROM TrackStudy t WHERE t.timestamp BETWEEN ?1 AND ?2",TrackStudy.class).setParameter(1, new Date(), TemporalType.DATE)
//                        .setParameter(2, cal, TemporalType.DATE);
        
        
        Query query = em.createNamedQuery("TrackStudy.findByUsername", TrackStudy.class).setParameter("username", username);
        return query.getResultList();
    
    } 
     
     
//     public List<TrackStudy> filterLatestStudy(){
//     
//         Date now = new Date();
//         Date oneMonth = new Date(now.getTime() - );
//         Query query = em.createQuery(null);
//     
//     }
//     
    public void persistStudy(TrackStudy study) {
        em.persist(study);
    }
    
    public void removeStudy(TrackStudy study) {
       em.remove(study);
    }
    
}
