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
    
    
    public long countStudyTeam(String name, String teamRole){
        return (Long)em.createNamedQuery("StudyTeam.countMastersByStudy").setParameter("name", name).setParameter("teamRole", teamRole).getSingleResult();
//        return (Long) em.createNativeQuery("SELECT COUNT(*) AS count FROM StudyTeam st WHERE st.name=? AND st.team_role=?",StudyTeam.class)
//                .setParameter(1, name).setParameter(2, "Guest").getSingleResult();
    }
    
    public List<StudyTeam> countMembersPerStudy(String name){
    
        Query query = em.createNamedQuery("StudyTeam.countAllMembers").setParameter("name", name);
        return query.getResultList();
    }
    
    public List<StudyTeam> findMembersByRole(String name, String role){
        Query query = em.createNamedQuery("StudyTeam.findMembersByRole", StudyTeam.class).setParameter("name", name).setParameter("teamRole", role);
        return query.getResultList();
    
    }
    
    //filter all members based on study name
    public List<StudyTeam> findMembersByStudy(String name){
        Query query = em.createNamedQuery("StudyTeam.findMembersByName", StudyTeam.class).setParameter("name", name);
        //Query query = em.createNativeQuery("select * from StudyTeam where name like \"BB%\"", StudyTeam.class); 
       
        return query.getResultList();
    
    }
    
    
    public void persistStudyTeam(StudyTeam team){
            em.persist(team);
    }
    
    public void removeStudyTeam(StudyTeam team){
            em.remove(team);
    }
    
    public void updateTeam(StudyTeam team){
            em.merge(team);
    }
    
    
    /**
     * Deletes all roles for the user on login, except ADMIN
     * @param username 
     */
    public void clearGroups(String username){
        em.createNamedQuery("UsersGroups.deleteGroupsForEmail", StudyTeam.class).setParameter("email", username).executeUpdate();
    }    
        
    public void setRoleForActiveStudy(String username, String studyname){
        // TODO
//        Query query = em.createNamedQuery("StudyTeam.findByNameAndTeamMember", StudyTeam.class).setParameter("name", studyname).setParameter("teamMember", username);
//        List<StudyTeam> res = query.getResultList();
//        assert(res.size()==1);
//        StudyTeam t = res.iterator().next();
//        em.createQuery("insert into USERS_GROUPS values('" + username + "'," + t.getTeamRole() + ")").executeUpdate();
    }    
}
