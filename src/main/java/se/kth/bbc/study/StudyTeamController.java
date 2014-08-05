/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author roshan
 */
@Stateless
public class StudyTeamController {
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }
    
    public StudyTeamController(){}
    
    
    public long countTeamMembers(String name){
        return (Long)em.createNamedQuery("TeamMembers.countByStudy").setParameter("name", name).getSingleResult();
    }
    
    public void persistStudyTeam(TeamMembers team){
            em.persist(team);
    }
    
    public void removeStudyTeam(TeamMembers team){
            em.remove(team);
    }
    
    public void updateTeam(TeamMembers team){
            em.merge(team);
    }
}
