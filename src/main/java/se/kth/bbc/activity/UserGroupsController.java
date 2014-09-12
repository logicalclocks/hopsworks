/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.activity;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


/**
 *
 * @author roshan
 */
@Stateless
public class UserGroupsController {
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }
    
    public UserGroupsController(){}
    
    
    public void persistUserGroups(UsersGroups userGroups){
            em.persist(userGroups);
    }
     
    public void removeUserGroups(String email, String groupname){
            UsersGroups ug = findByPrimaryKey(email, groupname);
            if(ug != null)
                 em.remove(ug);
    }
      
    public UsersGroups findByPrimaryKey(String email, String groupname) {
        return em.find(UsersGroups.class, new UsersGroups(new UsersGroupsPK(email, groupname)).getUsersGroupsPK());
    }

   
    /**
     * Deletes only GUEST role of a study from USERS_GROUPS table
     * @param username 
     */
    public void clearGroups(String email){
        em.createNamedQuery("UsersGroups.deleteGroupsForEmail", UsersGroups.class).setParameter("email", email).executeUpdate();
    }  
}
