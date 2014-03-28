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
    
    public List<Dataset> findAllDataSet(){
        return em.createNamedQuery("Dataset.findAll").getResultList();
    }
    
    
    public List<Dataset> findById(){
        return em.createNamedQuery("Dataset.getIds").getResultList();
    }
    
    
    public long getAllStudy(){
        return (Long)em.createNamedQuery("TrackStudy.countById").getSingleResult();
    }
    
    public List<DatasetStudy> findNameOwner() {
        TypedQuery<DatasetStudy> query = em.createNamedQuery("DatasetStudy.findByNameOwner", DatasetStudy.class);
        return query.getResultList();
    }
    
     public List<Dataset> findStudyOwner() {
        TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findOwnerById", Dataset.class);
        return query.getResultList();
    }
    
    
//     public List<Dataset> findAllById() {
//        //return em.createNamedQuery("Dataset.findOwnerById").getResultList();
//         return em.createQuery("SELECT d FROM Dataset d JOIN d.study s WHERE s.trackStudyPK.datasetId = :id").getResultList();
//     }
//    
    
    public void persistStudy(TrackStudy study) {
        em.persist(study);
    }
    
    public void removeStudy(TrackStudy study) {
       em.remove(study);
    }
    
    
    
}
