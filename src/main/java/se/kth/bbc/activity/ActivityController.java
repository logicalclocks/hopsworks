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
import javax.persistence.TypedQuery;

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
    public static final String REMOVED_SAMPLE = " removed a sample ";
    public static final String REMOVED_FILE = " removed a file ";
    public static final String REMOVED_STUDY = " removed study ";
    // Flag constants
    public static final String FLAG_STUDY = "STUDY";

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
    
    /**
     * Gets all activity information.
     * @return 
     */
    public List<ActivityDetail> getAllActivityDetail(){
        TypedQuery<ActivityDetail> q = em.createNamedQuery("ActivityDetail.findAll", ActivityDetail.class);
        return q.getResultList();
    }
    
    /**
     * Get all the activities performed on study <i>studyName</i>.
     * @param studyName
     * @return 
     */
    public List<ActivityDetail> activityDetailOnStudy(String studyName){
        TypedQuery<ActivityDetail> q = em.createNamedQuery("ActivityDetail.findByStudyname", ActivityDetail.class);
        q.setParameter("studyname", studyName);
        return q.getResultList();
    }
    
    /**
     * Returns all activity, but paginated. Items from <i>first</i> till
     * <i>first+pageSize</i> are returned.
     * @param first
     * @param pageSize
     * @return 
     */
    public List<ActivityDetail> getPaginatedActivityDetail(int first, int pageSize){
        TypedQuery<ActivityDetail>  q = em.createNamedQuery("ActivityDetail.findAll", ActivityDetail.class);
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        return q.getResultList();
    }
    
    /**
     * Returns all activities on study <i>studyName</i>, but paginated. Items from <i>first</i> till
     * <i>first+pageSize</i> are returned.
     * @param first
     * @param pageSize
     * @param filterStudy
     * @return 
     */
    public List<ActivityDetail> getPaginatedActivityDetailForStudy(int first,int pageSize, String studyName){
        TypedQuery<ActivityDetail> q = em.createNamedQuery("ActivityDetail.findByStudyname", ActivityDetail.class);
        q.setParameter("studyname", studyName);
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        return q.getResultList();
    }
    
    public long getTotalCount(){
        TypedQuery<Long> q = em.createNamedQuery("UserActivity.countAll",Long.class);
        return q.getSingleResult();
    }
    
    public long getStudyCount(String studyName){
        TypedQuery<Long> q = em.createNamedQuery("UserActivity.countStudy",Long.class);
        q.setParameter("studyName", studyName);
        return q.getSingleResult();
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
