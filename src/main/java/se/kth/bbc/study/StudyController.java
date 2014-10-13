/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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
    
    public TrackStudy findByName(String studyname){
        TypedQuery<TrackStudy> query = em.createNamedQuery("TrackStudy.findByName", TrackStudy.class).setParameter("name",studyname);
        TrackStudy result;
        try{
            result = query.getSingleResult();        
        } catch(NoResultException e){
            return null;
        }
        return result;
    }
    
    
    public int getAllStudy(String username){
        return ((Long)em.createNamedQuery("TrackStudy.findAllStudy").setParameter("username", username).getSingleResult()).intValue();
    }
    
    public int getMembers(String name){
        return ((Long)em.createNamedQuery("TrackStudy.findMembers").setParameter("name", name).getSingleResult()).intValue();
    }
    
    public List<TrackStudy> filterPersonalStudy(String username){
        
        Query query = em.createNamedQuery("TrackStudy.findByUsername", TrackStudy.class).setParameter("username", username);
        return query.getResultList();
    
    } 
     
     
     public String filterByName(String name){
     
         Query query = em.createNamedQuery("TrackStudy.findByName", TrackStudy.class).setParameter("name", name);
         List<TrackStudy> result = query.getResultList();
          if (result.iterator().hasNext()){
              TrackStudy t = result.iterator().next();
              return t.getUsername();
          }
              return null;
     }
     
     /**
        Get the owner of the given study. 
     */
     public String findOwner(String studyName){
         Query q = em.createNamedQuery("TrackStudy.findOwner", String.class).setParameter("name", studyName);
         return (String)q.getSingleResult();
     }
     
    
    public List<TrackStudy> findAllStudies(String user){
    
        Query query = em.createNativeQuery("SELECT name, username FROM study WHERE username=? UNION SELECT name, username FROM study WHERE name IN (SELECT name FROM StudyTeam WHERE team_member=?)",TrackStudy.class)
                .setParameter(1, user).setParameter(2, user);   
        
        return query.getResultList();
    }
    
    //Finds the details about all studies a user has joined
    public List<StudyDetail> findAllStudyDetails(String useremail){
        Query query = em.createNativeQuery("SELECT study.name as studyName, study.username as email, USERS.name as creator FROM study join USERS on study.username=USERS.email WHERE study.name IN (SELECT name FROM StudyTeam WHERE team_member=?)",StudyDetail.class)
                .setParameter(1, useremail); 
        return query.getResultList();
    }
    
    public List<StudyDetail> findAllPersonalStudyDetails(String useremail){
        Query query = em.createNativeQuery("SELECT study.name as studyName, study.username as email, USERS.name as creator FROM study join USERS on study.username=USERS.email WHERE study.username=?",StudyDetail.class)
                .setParameter(1, useremail); 
        return query.getResultList();
    }    
    
    public List<TrackStudy> findJoinedStudies(String user){
        
        Query query = em.createNativeQuery("select study.name, study.username from (StudyTeam join study on StudyTeam.name=study.name) join USERS on study.username=USERS.email where study.username not like ? and StudyTeam.team_member like ?", TrackStudy.class)
                .setParameter(1, user).setParameter(2, user);
        return query.getResultList();
    }
    
    public List<StudyDetail> findJoinedStudyDetails(String useremail){
        Query query = em.createNativeQuery("SELECT study.name AS studyName, study.username AS email, USERS.name AS creator FROM (StudyTeam JOIN study ON StudyTeam.name=study.name) JOIN USERS ON study.username=USERS.email WHERE study.username NOT LIKE ? AND StudyTeam.team_member LIKE ?", StudyDetail.class)
                .setParameter(1, useremail).setParameter(2,useremail);
       
        return query.getResultList();
    }
    
    public List<TrackStudy> QueryForNonRegistered(String user){
        
        Query query = em.createNativeQuery("SELECT name, username FROM study WHERE name IN (SELECT name FROM StudyTeam WHERE team_member=?)", TrackStudy.class)
                .setParameter(1, user);
        return query.getResultList();
    }
    
    public boolean checkForStudyOwnership(String user){

        Query query = em.createNamedQuery("TrackStudy.findAllStudy", TrackStudy.class).setParameter("username", user);
        long res = (Long) query.getSingleResult();
        
        return res>0;
    }
    
    
    
    public void persistStudy(TrackStudy study) {
        em.persist(study);
    }
    
    public void removeStudy(String name) {
       TrackStudy study = em.find(TrackStudy.class, name);
        if(study != null){
            em.remove(study);
        }
    }
    
    public synchronized void removeByName(String studyname){
        TrackStudy study = em.find(TrackStudy.class, studyname);
        if(study != null){
            em.remove(study);
        }
    }    

    public boolean findStudy(String name){
        TrackStudy study = em.find(TrackStudy.class, name);
        if(study != null) return true;
        else return false;
    }

}
