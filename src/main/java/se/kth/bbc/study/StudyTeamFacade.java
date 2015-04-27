/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study;

import java.util.ArrayList;
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
   * @param study
   * @param role
   * @return A list of User entities that have the role <i>role</i> in Study <i>study</i>.
   */
  public List<User> findTeamMembersByStudy(Study study, String role) {
    List<StudyTeam> results = findStudyTeamByStudyAndRole(study, role);
    ArrayList<User> retList = new ArrayList<>(results.size());
    for(StudyTeam st : results){
      retList.add(st.getUser());
    }
    return retList;
  }
  
  /**
   * Get all the StudyTeam entries for users in the role <i>role</i> in Study <i>study</i>.
   * @param study
   * @param role
   * @return 
   */
  public List<StudyTeam> findStudyTeamByStudyAndRole(Study study, String role){
    TypedQuery<StudyTeam> q = em.createNamedQuery("StudyTeam.findMembersByRoleInStudy",StudyTeam.class);
    q.setParameter("study", study);
    q.setParameter("teamRole",role);
    return q.getResultList();
  }

  /**
   * Find all the StudyTeam entries for members in Study <i>study</i> that have the role Researcher.
   * @param study
   * @return 
   */
  public List<StudyTeam> findResearchMembersByName(Study study) {
    return findStudyTeamByStudyAndRole(study, "Researcher");
  }

  /**
   * Find all the StudyTeam entries for members in Study <i>study</i> that have the role Guest.
   * @param study
   * @return 
   */
  public List<StudyTeam> findGuestMembersByName(Study study) {
    return findStudyTeamByStudyAndRole(study, "Guest");
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

  /**
   * Get the current role of User <i>user</i> in Study <i>study</i>.
   * @param study
   * @param user
   * @return The current role of the user in the study, or null if the user is not in it.
   */
  public String findCurrentRole(Study study, User user) {
    TypedQuery<StudyTeam> q = em.createNamedQuery("StudyTeam.findRoleForUserInStudy", StudyTeam.class);
    q.setParameter("study",study);
    q.setParameter("user", user);
    try{
      return q.getSingleResult().getTeamRole();
    }catch(NoResultException e){
      return null;
    }
  }
  
  /**
   * Get the current role of User <i>user</i> in Study <i>study</i>.
   * @param study
   * @param user
   * @return The current role of the user in the study, or null if the user is not in it.
   * @deprecated use findCurrentRole(Study study, User user) instead.
   */
  public String findCurrentRole(Study study, String user) {
    TypedQuery<User> q = em.createNamedQuery("User.findByEmail",User.class);
    q.setParameter("email", user);
    User u = q.getSingleResult();
    return findCurrentRole(study,u);
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

  /**
   * Check if the User <i>user</i> is a member of the Study <i>study</i>.
   * @param study
   * @param user
   * @return 
   */
  public boolean isUserMemberOfStudy(Study study, User user) {
    TypedQuery<StudyTeam> q = em.createNamedQuery("StudyTeam.findRoleForUserInStudy",StudyTeam.class);
    q.setParameter("study", study);
    q.setParameter("user", user);
    return q.getResultList().size() > 0;
  }
 
  /**
   * Check if the User <i>user</i> is a member of the Study <i>study</i>.
   * @param study
   * @param user
   * @return 
   * @deprecated use isUserMemberOfStudy(Study study,User user) instead.
   */
  public boolean isUserMemberOfStudy(Study study, String user) {
    TypedQuery<User> q = em.createNamedQuery("User.findByEmail", User.class);
    q.setParameter("email", user);
    return isUserMemberOfStudy(study, q.getSingleResult());
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
