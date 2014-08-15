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
    
        Query query = em.createNamedQuery("UserActivity.findAll", UserActivity.class);
        return query.getResultList();
    }
    
    public List<UserActivity> activityOnstudy(String activityOn){
    
        Query query = em.createNamedQuery("UserActivity.findByActivityOn", UserActivity.class).setParameter("activityOn", activityOn);
        return query.getResultList();
    }
    
    
    
}
