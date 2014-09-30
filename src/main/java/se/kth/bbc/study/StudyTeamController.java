/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.util.Date;
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
    
    
    public int countStudyTeam(String name, String teamRole){
        return ((Long)em.createNamedQuery("StudyTeam.countMastersByStudy").setParameter("name", name)
                .setParameter("teamRole", teamRole).getSingleResult()).intValue();
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
    
    
    public List<TrackStudy> findStudyMaster(String name){
        Query query = em.createNativeQuery("SELECT * FROM study WHERE name IN (SELECT name FROM StudyTeam WHERE name=?)", TrackStudy.class).setParameter(1, name);
        return query.getResultList();
    }
    
    public List<StudyTeam> findByMember(String teamMember){
    
        Query query = em.createNamedQuery("StudyTeam.findByTeamMember", StudyTeam.class).setParameter("teamMember", teamMember).setParameter("teamRole", "Master");
        return query.getResultList();
    }
    
    
    public List<StudyTeam> findCurrentRole(String name, String username){
    
        Query query = em.createNativeQuery("SELECT * FROM StudyTeam where name=? AND team_member=?",StudyTeam.class).setParameter(1, name).setParameter(2, username);
        return query.getResultList();
    
    }
    
    public void persistStudyTeam(StudyTeam team){
            em.persist(team);
    }
    
    public void removeStudyTeam(String name, String email){
        StudyTeam team = findByPrimaryKey(name, email);
            if(team != null)
                em.remove(team);
    }
    
    public void updateTeamRole(String name, String email, String teamRole){
        //System.out.println(teamRole + "size --- "+ teamRole.length());
        StudyTeam team = findByPrimaryKey(name, email);
            if(team != null){
                team.setTeamRole(teamRole);
                team.setTimestamp(new Date());
                em.merge(team);
            }
            
//            em.createNamedQuery("StudyTeam.updateTeamRole", StudyTeam.class).setParameter("teamRole", teamRole).setParameter("timestamp", new Date())
//                    .setParameter("name", name).setParameter("teamMember", email).executeUpdate();
            
//            Query query = em.createNativeQuery("UPDATE StudyTeam SET team_role= '"+ teamRole +"' , timestamp='"+ new Date() +"' WHERE team_member=? AND name=?",StudyTeam.class)
//                    .setParameter(1, email).setParameter(2, name);
//            
//            em.merge(query);
//           
            
    }
    
    
    public StudyTeam findByPrimaryKey(String name, String email) {
        return em.find(StudyTeam.class, new StudyTeam(new StudyTeamPK(name, email)).getStudyTeamPK());
    }
    
      
        
    public boolean findUserForActiveStudy(String studyname, String username){
        // TODO
//        Query query = em.createNamedQuery("StudyTeam.findByNameAndTeamMember", StudyTeam.class).setParameter("name", studyname).setParameter("teamMember", username);
        Query query = em.createNativeQuery("SELECT * FROM StudyTeam WHERE name =? AND team_member=?", StudyTeam.class).setParameter(1, studyname).setParameter(2, username);
        //List<StudyTeam> res = query.getResultList();
        if( query.getResultList().size() > 0)
            return true;
        
        return false;
//        //System.out.println("Returned " + res.size() + " answers!!");
//        if (res.iterator().hasNext()) {
//            StudyTeam t = res.iterator().next();
//                //em.createQuery("insert into USERS_GROUPS values('" + username + "'," + t.getTeamRole() + ")").executeUpdate();
//                  Query update = em.createNativeQuery("INSERT INTO USERS_GROUPS VALUES(?,?)", UsersGroups.class).setParameter(1, t.studyTeamPK.getTeamMember()).setParameter(2, t.getTeamRole());
//                  em.persist(update);
//        }
    }    
    
    public StudyTeam getStudyTeam(String studyname, String username){
        Query query = em.createNativeQuery("SELECT * FROM StudyTeam WHERE name =? AND team_member=?", StudyTeam.class).setParameter(1, studyname).setParameter(2, username);
        List<StudyTeam> res = query.getResultList();
        if (res.iterator().hasNext()) {
            StudyTeam t = res.iterator().next();
            return t;
        }
            return null;
    }  
      
}
