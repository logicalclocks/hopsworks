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
        TypedQuery<TrackStudy> query = em.createNamedQuery("Samples.findAll", TrackStudy.class);
        return query.getResultList();
    }
    
    public void persistStudy(TrackStudy study) {
        em.persist(study);
    }
    
    public void removeStudy(TrackStudy study) {
       em.remove(study);
    }
    
    
    
}
