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
import javax.persistence.NoResultException;
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

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public StudyTeamFacade() {
  }

  /**
   * Count the number of members in this study with the given role.
   * <p>
   * @param study
   * @param role
   * @return
   */
  public int countStudyTeam(Study study, String role) {
    TypedQuery<Long> query = em.createNamedQuery(
            "StudyTeam.countMembersForStudyAndRole", Long.class);
    query.setParameter("study", study);
    query.setParameter("teamRole", role);
    return query.getSingleResult().intValue();
  }

  /**
   * Count the number of members in this study.
   * <p>
   * @param study
   * @return
   */
  public int countMembersInStudy(Study study) {
    TypedQuery<Long> query = em.createNamedQuery(
            "StudyTeam.countAllMembersForStudy", Long.class);
    query.setParameter("study", study);
    return query.getSingleResult().intValue();
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

  /**
   * Get all the StudyTeam entries for the given study.
   * <p>
   * @param study
   * @return
   */
  public List<StudyTeam> findMembersByStudy(Study study) {
    TypedQuery<StudyTeam> query = em.createNamedQuery("StudyTeam.findByStudy",
            StudyTeam.class);
    query.setParameter("study", study);
    return query.getResultList();
  }

  public List<Study> findStudyMaster(String name) {
    Query query = em.createNativeQuery(
            "SELECT * FROM study WHERE name IN (SELECT name FROM study_team WHERE name=?)",
            Study.class).setParameter(1, name);
    return query.getResultList();
  }

  /**
   * Find all StudyTeam entries containing the given User as member.
   * <p>
   * @param member
   * @return
   */
  public List<StudyTeam> findByMember(User member) {
    Query query = em.createNamedQuery("StudyTeam.findByTeamMember",
            StudyTeam.class).setParameter("user", member);
    return query.getResultList();
  }

  /**
   * Count the number of studies this user is a member of.
   * <p>
   * @param user
   * @return
   */
  public int countByMember(User user) {
    TypedQuery<Long> query = em.createNamedQuery(
            "StudyTeam.countStudiesByMember", Long.class);
    query.setParameter("user", user);
    return query.getSingleResult().intValue();
  }

  /**
   * Count the number of studies the user with this email address is a member
   * of.
   * <p>
   * @param email
   * @return
   * @deprecated Use countByMember(User user) instead.
   */
  public int countByMemberEmail(String email) {
    TypedQuery<User> query = em.createNamedQuery(
            "User.findByEmail", User.class);
    query.setParameter("email", email);
    User user = query.getSingleResult();
    return countByMember(user);
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
    // TODO: use named query
    Query query = em.createNativeQuery(
            "SELECT * FROM study_team WHERE name =? AND team_member=?",
            StudyTeam.class).setParameter(1, studyname).
            setParameter(2, username);
    if (query.getResultList().size() > 0) {
      return true;
    }
    return false;
  }

  /**
   * Find the StudyTeam entry for the given study and user.
   * <p>
   * @param study
   * @param user
   * @return The StudyTeam entry, null if it does not exist.
   */
  public StudyTeam findStudyTeam(Study study, User user) {
    TypedQuery<StudyTeam> q = em.createNamedQuery(
            "StudyTeam.findRoleForUserInStudy",
            StudyTeam.class);
    q.setParameter("user", user);
    q.setParameter("study", study);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
