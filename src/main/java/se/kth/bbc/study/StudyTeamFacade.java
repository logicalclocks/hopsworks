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
import javax.persistence.TypedQuery;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author roshan
 */
@Stateless
public class StudyTeamFacade {

  @PersistenceContext(unitName = "hopsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public StudyTeamFacade() {
  }

  public int countStudyTeam(String name, String teamRole) {
    return ((Long) em.createNamedQuery("StudyTeam.countMastersByStudy").
            setParameter("name", name)
            .setParameter("teamRole", teamRole).getSingleResult()).intValue();
  }

  public List<StudyTeam> countMembersPerStudy(String name) {

    Query query = em.createNamedQuery("StudyTeam.countAllMembers").setParameter(
            "name", name);
    return query.getResultList();
  }

  /**
   * Find all team members in study 'name' with role 'role'.
   * <p>
   * @param name
   * @param role
   * @return
   */
  public List<User> findTeamMembersByName(String name, String role) {
    Query query = em.createNativeQuery(
            "SELECT fname,lname,email,uid FROM users WHERE email IN (SELECT team_member FROM study_team WHERE name=? AND team_role=?)",
            User.class)
            .setParameter(1, name).setParameter(2, role);
    return query.getResultList();

  }

  public List<StudyTeam> findResearchMembersByName(String name) {
    Query query = em.createNativeQuery(
            "SELECT * FROM study_team WHERE name =? AND team_role=?",
            StudyTeam.class).setParameter(1, name).setParameter(2, "Researcher");
    return query.getResultList();
  }

  public List<StudyTeam> findGuestMembersByName(String name) {
    Query query = em.createNativeQuery(
            "SELECT * FROM study_team WHERE name =? AND team_role=?",
            StudyTeam.class).setParameter(1, name).setParameter(2, "Guest");
    return query.getResultList();
  }

  //filter all members based on study name
  public List<StudyTeam> findMembersByStudy(String name) {
    Query query = em.createNamedQuery("StudyTeam.findMembersByName",
            StudyTeam.class).setParameter("name", name);
    return query.getResultList();

  }

  public List<TrackStudy> findStudyMaster(String name) {
    Query query = em.createNativeQuery(
            "SELECT * FROM study WHERE name IN (SELECT name FROM study_team WHERE name=?)",
            TrackStudy.class).setParameter(1, name);
    return query.getResultList();
  }

  /*
   * Finds all studies in which the user teamMember is a participant (no matter
   * which role).
   */
  public List<StudyTeam> findByMember(String teamMember) {
    Query query = em.createNamedQuery("StudyTeam.findByTeamMember",
            StudyTeam.class).setParameter("teamMember", teamMember);
    return query.getResultList();
  }

  public long countByMember(String teamMember) {
    Query query = em.createNamedQuery("StudyTeam.countStudiesByMember",
            Long.class).setParameter("teamMember", teamMember);
    return (Long) query.getSingleResult();
  }

  public List<StudyTeam> findCurrentRole(String name, String username) {

    Query query = em.createNativeQuery(
            "SELECT * FROM study_team where name=? AND team_member=?",
            StudyTeam.class).setParameter(1, name).setParameter(2, username);
    return query.getResultList();

  }

  public void persistStudyTeam(StudyTeam team) {
    em.persist(team);
  }

  public void removeStudyTeam(String name, String email) {
    StudyTeam team = findByPrimaryKey(name, email);
    if (team != null) {
      em.remove(team);
    }
  }

  public void updateTeamRole(String name, String email, String teamRole) {
    StudyTeam team = findByPrimaryKey(name, email);
    if (team != null) {
      team.setTeamRole(teamRole);
      team.setTimestamp(new Date());
      em.merge(team);
    }
  }

  public StudyTeam findByPrimaryKey(String name, String email) {
    return em.find(StudyTeam.class, new StudyTeam(new StudyTeamPK(name, email)).
            getStudyTeamPK());
  }

  public boolean findUserForActiveStudy(String studyname, String username) {
    // TODO
//        Query query = em.createNamedQuery("StudyTeam.findByNameAndTeamMember", StudyTeam.class).setParameter("name", studyname).setParameter("teamMember", username);
    Query query = em.createNativeQuery(
            "SELECT * FROM study_team WHERE name =? AND team_member=?",
            StudyTeam.class).setParameter(1, studyname).
            setParameter(2, username);
    //List<StudyTeam> res = query.getResultList();
    if (query.getResultList().size() > 0) {
      return true;
    }

    return false;
//        //System.out.println("Returned " + res.size() + " answers!!");
//        if (res.iterator().hasNext()) {
//            StudyTeam t = res.iterator().next();
//                //em.createQuery("insert into USERS_GROUPS values('" + username + "'," + t.getTeamRole() + ")").executeUpdate();
//                  Query update = em.createNativeQuery("INSERT INTO USERS_GROUPS VALUES(?,?)", UsersGroups.class).setParameter(1, t.studyTeamPK.getTeamMember()).setParameter(2, t.getTeamRole());
//                  em.persist(update);
//        }
  }

  /**
   * Find the StudyTeam entry with studyName and username as primary key.
   * <p>
   * @param studyName
   * @param username
   * @return
   */
  public StudyTeam findStudyTeam(String studyName, String username) {
    TypedQuery<StudyTeam> q = em.createNamedQuery("StudyTeam.find",
            StudyTeam.class);
    q.setParameter("studyName", studyName).setParameter("username", username);
    return q.getSingleResult();
  }

}
