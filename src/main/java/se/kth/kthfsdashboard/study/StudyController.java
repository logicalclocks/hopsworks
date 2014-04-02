/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

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
    
    public List<String> findOwner(String username) {
        String query = "SELECT t.name, u.name FROM TrackStudy t, Username u WHERE t.username = u.email AND t.username = :username";
        return em.createQuery(query).setParameter("username", username).getResultList();
    }
    
    
    
//    public List<Object> findOwner() {
//        String query = "SELECT t.name, u.name FROM TrackStudy t, Username u WHERE t.username = u.email";
//        return em.createQuery(query).getResultList();
//    }
//    
//     public List<Dataset> findStudyOwner() {
//        TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findOwnerById", Dataset.class);
//        return query.getResultList();
//    }
    
    
//     public List<Dataset> findAllById() {
//        //return em.createNamedQuery("DatasetOld.findOwnerById").getResultList();
//         return em.createQuery("SELECT d FROM DatasetOld d JOIN d.study s WHERE s.trackStudyPK.datasetId = :id").getResultList();
//     }
//    
    
    public void persistStudy(TrackStudy study) {
        em.persist(study);
    }
    
    public void removeStudy(TrackStudy study) {
       em.remove(study);
    }
    
    
    
}
