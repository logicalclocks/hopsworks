/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

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
public class StudyTeamController {
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }
    
    public StudyTeamController(){}
    
    
    public long countTeamMembers(String name, String teamRole){
        return (Long)em.createNamedQuery("TeamMembers.countMastersByStudy").setParameter("name", name).setParameter("teamRole", teamRole).getSingleResult();
//        return (Long) em.createNativeQuery("SELECT COUNT(*) AS count FROM StudyTeam st WHERE st.name=? AND st.team_role=?",TeamMembers.class)
//                .setParameter(1, name).setParameter(2, "Guest").getSingleResult();
    }
    
    public List<TeamMembers> countMembersPerStudy(String name){
    
        Query query = em.createNamedQuery("TeamMembers.countAllMembers").setParameter("name", name);
        return query.getResultList();
    }
    
    public List<TeamMembers> findMembersByRole(String name, String role){
        Query query = em.createNamedQuery("TeamMembers.findMembersByRole", TeamMembers.class).setParameter("name", name).setParameter("teamRole", role);
        return query.getResultList();
    
    }
    
     public List<TeamMembers> findMembersByStudy(String name){
        Query query = em.createNamedQuery("TeamMembers.findMembersByName", TeamMembers.class).setParameter("name", name);
        return query.getResultList();
    
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
