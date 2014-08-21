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
    
    public List<TrackStudy> findAllStudies(String user){
    
        //Query query = em.createNativeQuery("SELECT name, username FROM study WHERE username=? UNION SELECT name, team_member FROM StudyTeam WHERE team_member=?",TrackStudy.class).setParameter(1, user).setParameter(2, user);
        //select name, username from study where username='roshan@yahoo.com' UNION ALL select name, username from study where name IN (select name from StudyTeam where team_member='roshan@yahoo.com' AND team_role != 'Master');

        Query query = em.createNativeQuery("SELECT name, username FROM study WHERE username=? UNION ALL SELECT name, username FROM study WHERE name IN (SELECT name FROM StudyTeam WHERE team_member=? AND team_role != 'Master')",TrackStudy.class)
                .setParameter(1, user).setParameter(2, user);
        
        return query.getResultList();
    }
    
    
    public List<TrackStudy> findJoinedStudies(String user){
    
        //SELECT name, username FROM study WHERE NOT EXISTS (SELECT * FROM StudyTeam WHERE StudyTeam.team_member=? AND study.username=?)
        //select name, username from study where name IN (select name from StudyTeam where team_member='roshan@yahoo.com' and team_role != 'Master');
        Query query = em.createNativeQuery("SELECT name, username FROM study WHERE name IN (SELECT name FROM StudyTeam WHERE team_member=? AND team_role != 'Master')", TrackStudy.class)
                .setParameter(1, user);
        return query.getResultList();
    }
    
    
    public void persistStudy(TrackStudy study) {
        em.persist(study);
    }
    
    public void removeStudy(TrackStudy study) {
       em.remove(study);
    }
    
}
