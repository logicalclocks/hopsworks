package se.kth.bbc.study;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityDetail;
import se.kth.bbc.study.privacy.model.Consent;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author roshan
 */
@Stateless
public class StudyFacade extends AbstractFacade<Study> {

  private static final Logger logger = Logger.getLogger(StudyFacade.class.
          getName());

  @PersistenceContext(unitName = "hopsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  @EJB
  private StudyTeamFacade stc;

  private List<ActivityDetail> ad;

  @EJB
  private ActivityController activityController;

  public StudyFacade() {
    super(Study.class);
  }

  public List<Study> findAll() {
    TypedQuery<Study> query = em.createNamedQuery("Study.findAll",
            Study.class);
    return query.getResultList();
  }

  public List<Study> findByUser(String username) {
    TypedQuery<Study> query = em.createNamedQuery(
            "Study.findByUsername", Study.class).setParameter(
                    "username", username);
    return query.getResultList();
  }

  public Study findByName(String studyname) {
    TypedQuery<Study> query = em.createNamedQuery("Study.findByName",
            Study.class).setParameter("name", studyname);
    Study result;
    try {
      result = query.getSingleResult();
      return result;
    } catch (NoResultException e) {
      return null;
    }
  }

  public int getAllStudy(String username) {
    return ((Long) em.createNamedQuery("Study.countStudyByOwner").
            setParameter("username", username).getSingleResult()).intValue();
  }

  public int getMembers(String name) {
    return ((Long) em.createNamedQuery("Study.findMembers").setParameter(
            "name", name).getSingleResult()).intValue();
  }

  public List<Study> filterPersonalStudy(String username) {

    Query query = em.createNamedQuery("Study.findByUsername",
            Study.class).setParameter("username", username);
    return query.getResultList();

  }

  public String filterByName(String name) {

    Query query
            = em.createNamedQuery("Study.findByName", Study.class).
            setParameter("name", name);
    List<Study> result = query.getResultList();
    if (result.iterator().hasNext()) {
      Study t = result.iterator().next();
      return t.getUsername();
    }
    return null;
  }

  /**
   * Get the owner of the given study.
   */
  public String findOwner(String studyName) {
    Query q = em.createNamedQuery("Study.findOwner", String.class).
            setParameter("name", studyName);
    return (String) q.getSingleResult();
  }

  public List<Study> findAllStudies(String user) {

    Query query = em.createNativeQuery(
            "SELECT name, username FROM study WHERE username=? UNION SELECT name, username FROM study WHERE name IN (SELECT name FROM study_team WHERE team_member=?)",
            Study.class)
            .setParameter(1, user).setParameter(2, user);

    return query.getResultList();
  }

  /**
   * Find details about all the studies a user has joined.
   * <p>
   * @param useremail
   * @return
   */
  public List<StudyDetail> findAllStudyDetails(String useremail) {
    Query query = em.createNativeQuery(
            "SELECT * FROM study_details WHERE studyname IN (SELECT name FROM study_team WHERE team_member=?)",
            StudyDetail.class)
            .setParameter(1, useremail);
    return query.getResultList();
  }

  /**
   * Find all studies created (and owned) by this user.
   * <p>
   * @param useremail
   * @return
   */
  public List<StudyDetail> findAllPersonalStudyDetails(String useremail) {
    TypedQuery<StudyDetail> q = em.createNamedQuery("StudyDetail.findByEmail",
            StudyDetail.class);
    q.setParameter("email", useremail);
    return q.getResultList();
  }

  //TODO: remove this method and replace with findJoinedStudyDetails
  public List<Study> findJoinedStudies(String user) {

    Query query = em.createNativeQuery(
            "select study.name, study.username from (study_team join study on study_team.name=study.name) join users on study.username=users.email where study.username not like ? and study_team.team_member like ?",
            Study.class)
            .setParameter(1, user).setParameter(2, user);
    return query.getResultList();
  }

  /**
   * Get all the studies this user has joined, but not created.
   * <p>
   * @param useremail
   * @return
   */
  public List<StudyDetail> findJoinedStudyDetails(String useremail) {
    Query query = em.createNativeQuery(
            "SELECT * FROM study_details WHERE studyname IN (SELECT name FROM study_team WHERE team_member=?) AND email NOT LIKE ?",
            StudyDetail.class)
            .setParameter(1, useremail).setParameter(2, useremail);

    return query.getResultList();
  }

  public List<Study> QueryForNonRegistered(String user) {

    Query query = em.createNativeQuery(
            "SELECT name, username FROM study WHERE name IN (SELECT name FROM study_team WHERE team_member=?)",
            Study.class)
            .setParameter(1, user);
    return query.getResultList();
  }

  public boolean checkForStudyOwnership(String user) {

    Query query = em.createNamedQuery("Study.countStudyByOwner",
            Study.class).setParameter("username", user);
    long res = (Long) query.getSingleResult();

    return res > 0;
  }

  public void persistStudy(Study study) {
    em.persist(study);
  }

  public void removeStudy(String name) {
    Study study = em.find(Study.class, name);
    if (study != null) {
      em.remove(study);
    }
  }

  public synchronized void removeByName(String studyname) {
    Study study = em.find(Study.class, studyname);
    if (study != null) {
      em.remove(study);
    }
  }

  public boolean findStudy(String name) {
    Study study = em.find(Study.class, name);
    return study != null;
  }

  public boolean updateRetentionPeriod(String name, Date date) {

    Study study = em.find(Study.class, name);
    study.setRetentionPeriod(date);
    if (study != null) {
      em.merge(study);
      return true;
    }
    return false;
  }

  public Date getRetentionPeriod(String name) {
    Study study = em.find(Study.class, name);
    if (study != null) {
      return study.getRetentionPeriod();
    }
    return null;
  }

}
