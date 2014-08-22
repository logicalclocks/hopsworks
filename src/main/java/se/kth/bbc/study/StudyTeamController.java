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
import se.kth.kthfsdashboard.user.Username;

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
    }
    
    public List<StudyTeam> countMembersPerStudy(String name){
    
        Query query = em.createNamedQuery("StudyTeam.countAllMembers").setParameter("name", name);
        return query.getResultList();
    }
    
    public List<Username> findMasterMembersByName(String name, String role){
        //Query query = em.createNamedQuery("StudyTeam.findMembersByRole", StudyTeam.class).setParameter("name", name).setParameter("teamRole", role);
        // select NAME, EMAIL from USERS where EMAIL in (select team_member from StudyTeam where name='BBC' and team_role='Guests')
        //SELECT * FROM StudyTeam WHERE name =? AND team_role=?
        Query query = em.createNativeQuery("SELECT NAME, EMAIL FROM USERS WHERE EMAIL IN (SELECT team_member FROM StudyTeam WHERE name=? AND team_role=?)" , Username.class).setParameter(1, name).setParameter(2, role);
        return query.getResultList();
    
    }
    
    
    public List<Username> findTeamMembersByName(String name, String role){
        Query query = em.createNativeQuery("SELECT NAME, EMAIL FROM USERS WHERE EMAIL IN (SELECT team_member FROM StudyTeam WHERE name=? AND team_role=?)" , Username.class)
                    .setParameter(1, name).setParameter(2, role);
        return query.getResultList();
    
    }
    
    
    public List<StudyTeam> findResearchMembersByName(String name){
        //Query query = em.createNamedQuery("StudyTeam.findMembersByRole", StudyTeam.class).setParameter("name", name).setParameter("teamRole", role);
        Query query = em.createNativeQuery("SELECT * FROM StudyTeam WHERE name =? AND team_role=?" , StudyTeam.class).setParameter(1, name).setParameter(2, "Researcher");
        return query.getResultList();
    
    }
    
    public List<StudyTeam> findGuestMembersByName(String name){
        //Query query = em.createNamedQuery("StudyTeam.findMembersByRole", StudyTeam.class).setParameter("name", name).setParameter("teamRole", role);
        Query query = em.createNativeQuery("SELECT * FROM StudyTeam WHERE name =? AND team_role=?" , StudyTeam.class).setParameter(1, name).setParameter(2, "Guest");
        return query.getResultList();
    
    }
    
    //filter all members based on study name
    public List<StudyTeam> findMembersByStudy(String name){
        Query query = em.createNamedQuery("StudyTeam.findMembersByName", StudyTeam.class).setParameter("name", name);
        return query.getResultList();
    
    }
    
    
    public List<TrackStudy> findStudyMaster(String email){
    
        Query query = em.createNativeQuery("SELECT * FROM study WHERE username=?", TrackStudy.class).setParameter(1, email);
        return query.getResultList();
    }
    
    public List<StudyTeam> findByMember(String teamMember){
    
        Query query = em.createNamedQuery("StudyTeam.findByTeamMember", StudyTeam.class).setParameter("teamMember", teamMember).setParameter("teamRole", "Master");
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
        Query query = em.createNativeQuery("SELECT * FROM StudyTeam WHERE name =? AND team_member=?", StudyTeam.class).setParameter(1, studyname).setParameter(2, username);
        List<StudyTeam> res = query.getResultList();
        //System.out.println("Returned " + res.size() + " answers!!");
        if (res.iterator().hasNext()) {
            StudyTeam t = res.iterator().next();
            em.createQuery("insert into USERS_GROUPS values('" + username + "'," + t.getTeamRole() + ")").executeUpdate();
        }
    }    
}
