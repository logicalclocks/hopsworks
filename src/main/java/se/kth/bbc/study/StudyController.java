/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.time.DateUtils;
import se.kth.kthfsdashboard.user.Username;

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
    
    
    public long getAllStudy(String username){
        return (Long)em.createNamedQuery("TrackStudy.findAllStudy").setParameter("username", username).getSingleResult();
    }
    
    public long getMembers(String name){
        return (Long)em.createNamedQuery("TrackStudy.findMembers").setParameter("name", name).getSingleResult();
    }
    
    public List<TrackStudy> filterPersonalStudy(String username){
    
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(new Date());
//        cal.add(Calendar.DATE, -2);
//        
//        Query query = 
//                em.createQuery("SELECT t FROM TrackStudy t WHERE t.timestamp BETWEEN ? AND ?",TrackStudy.class).setParameter(1, new Date(), TemporalType.DATE)
//                        .setParameter(2, cal, TemporalType.DATE);
        
        
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
     
    
    public List<TrackStudy> findAllStudies(String user){
    
        //Query query = em.createNativeQuery("SELECT name, username FROM study WHERE username=? UNION SELECT name, team_member FROM StudyTeam WHERE team_member=?",TrackStudy.class).setParameter(1, user).setParameter(2, user);
        //select name, username from study where username='roshan@yahoo.com' UNION ALL select name, username from study where name IN (select name from StudyTeam where team_member='roshan@yahoo.com');

        Query query = em.createNativeQuery("SELECT name, username FROM study WHERE username=? UNION SELECT name, username FROM study WHERE name IN (SELECT name FROM StudyTeam WHERE team_member=?)",TrackStudy.class)
                .setParameter(1, user).setParameter(2, user);
        
        return query.getResultList();
    }
    
    //SELECT name, username FROM study WHERE name IN (SELECT DISTINCT name FROM StudyTeam WHERE name NOT IN (SELECT name FROM study WHERE username=?) "
    //            + "AND (SELECT COUNT(*) FROM study where username=?) > 0)
    
    
    public List<TrackStudy> findJoinedStudies(String user){
    
        //select name, username from study where name IN (select distinct name from StudyTeam where name NOT IN (select name from study where username like 'roshan%'))
        //select name, username from study where (name,username) NOT IN (select name, team_member from StudyTeam where team_member='roshan@yahoo.com')
        
        Query query = em.createNativeQuery("SELECT name, username FROM study WHERE (name, username) NOT IN (SELECT name, team_member FROM StudyTeam WHERE team_member=?)", TrackStudy.class)
                .setParameter(1, user);
        return query.getResultList();
    }
    
    public List<TrackStudy> QueryForNonRegistered(String user){
        
        Query query = em.createNativeQuery("SELECT name, username FROM study WHERE name IN (SELECT name FROM StudyTeam WHERE team_member=?)", TrackStudy.class)
                .setParameter(1, user);
        return query.getResultList();
    }
    
    public boolean checkForStudyOwnership(String user){
    
        //Query query = em.createNativeQuery("SELECT EXISTS(SELECT * from study WHERE username=?) AS T", TrackStudy.class).setParameter(1, user);
        Query query = em.createNamedQuery("TrackStudy.findAllStudy", TrackStudy.class).setParameter("username", user);
        long res = (Long) query.getSingleResult();
        
        if(res > 0) return true;
                else return false;
    }
    
    
    
    public void persistStudy(TrackStudy study) {
        em.persist(study);
    }
    
    public void removeStudy(TrackStudy study) {
        em.remove(study);
    }
    
    public synchronized void removeByName(String studyname){
        TrackStudy study = em.find(TrackStudy.class, studyname);
        if(study != null){
            em.remove(study);
        }
    }    
}
