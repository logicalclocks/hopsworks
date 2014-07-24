/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.activity;

import java.util.ArrayList;
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
    
//        return em.createNamedQuery("nativeActivity").getResultList().toArray();
        
        Query query = em.createNativeQuery("SELECT id, activity, performed_by, activity_on FROM activity", UserActivity.class);
        //Query query = em.createNativeQuery("SELECT * FROM activity act, USERS u WHERE act.performed_By = u.EMAIL","activityList");
        return query.getResultList();
        //act.id, act.activity, act.performed_by, act.activity_on, u.NAME 
    }
    
    
}
