/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.activity;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author roshan
 */
@Stateless
public class ActivityController {
    
    // String constants
    public static final String NEW_STUDY = " created new study ";
    public static final String NEW_DATA = " added a new dataset ";
    public static final String NEW_MEMBER = " added new member ";
    public static final String NEW_SAMPLE = " added a new sample ";
    public static final String CHANGE_ROLE = " changed role of ";
    public static final String REMOVED_MEMBER = " removed team member ";
    public static final String REMOVED_STUDY = " removed study ";
    // Flag constants
    public static final String CTE_FLAG_STUDY = "STUDY";

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }
        
    public ActivityController() {
    }
    
    public void persistActivity(UserActivity activity) {
        em.persist(activity);
    }
    
    public void removetActivity(UserActivity activity) {
        em.remove(activity);
    }

    public List<UserActivity> filterActivity(){    
        Query query = em.createNamedQuery("UserActivity.findAll", UserActivity.class);
        return query.getResultList();
    }
    
    public List<ActivityDetail> filterActivityDetail(){
        Query query = em.createNativeQuery("SELECT id, performed_By AS email, USERS.name AS author, activity, activity_on AS studyName, timestamp AS myTimestamp FROM activity JOIN USERS ON activity.performed_By=USERS.email ORDER BY myTimestamp DESC", ActivityDetail.class);
        return query.getResultList();
    }
    
    public List<UserActivity> activityOnstudy(String activityOn){
    
        Query query = em.createNamedQuery("UserActivity.findByActivityOn", UserActivity.class).setParameter("activityOn", activityOn);
        return query.getResultList();
    }
    
    public List<ActivityDetail> activityDetailOnStudy(String studyName){
        Query query = em.createNativeQuery("SELECT id, performed_By AS email, USERS.name AS author, activity, activity_on AS studyName, timestamp AS myTimestamp FROM activity JOIN USERS ON activity.performed_By=USERS.email WHERE activity.activity_on = ? ORDER BY myTimestamp DESC", ActivityDetail.class);
        query.setParameter(1, studyName);
        return query.getResultList();
    }
    
    public List<UserActivity> activityOnID(int id){
    
        Query query = em.createNamedQuery("UserActivity.findById", UserActivity.class).setParameter("id", id);
        return query.getResultList();
    }
    
    public List<UserActivity> lastActivityOnStudy(String name){
        Query query = em.createNativeQuery("SELECT * FROM activity WHERE activity_on=? ORDER BY timestamp DESC LIMIT 1", UserActivity.class).setParameter(1, name);
        return query.getResultList();
    }
    
    public List<UserActivity> findAllTeamActivity(String flag){
        Query query = em.createNamedQuery("UserActivity.findByFlag",UserActivity.class).setParameter("flag", flag);
        return query.getResultList();
    }

}
