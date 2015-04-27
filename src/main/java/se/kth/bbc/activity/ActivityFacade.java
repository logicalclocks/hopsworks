package se.kth.bbc.activity;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.study.Study;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author roshan
 */
@Stateless
public class ActivityFacade extends AbstractFacade<Activity> {

  private static final Logger logger = Logger.getLogger(ActivityFacade.class.
          getName());

  // String constants
  public static final String NEW_STUDY = " created a new study ";
  public static final String NEW_DATA = " added a new dataset ";
  public static final String NEW_MEMBER = " added a member ";
  public static final String NEW_SAMPLE = " added a new sample ";
  public static final String CHANGE_ROLE = " changed the role of ";
  public static final String REMOVED_MEMBER = " removed team member ";
  public static final String REMOVED_SAMPLE = " removed a sample ";
  public static final String REMOVED_FILE = " removed a file ";
  public static final String REMOVED_STUDY = " removed study ";
  public static final String RAN_JOB = " ran a job ";
  // Flag constants
  public static final String FLAG_STUDY = "STUDY";

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ActivityFacade() {
    super(Activity.class);
  }

  public void persistActivity(Activity activity) {
    em.persist(activity);
  }

  public void removeActivity(Activity activity) {
    em.remove(activity);
  }

  public long getTotalCount() {
    TypedQuery<Long> q = em.
            createNamedQuery("Activity.countAll", Long.class);
    return q.getSingleResult();
  }

  public long getStudyCount(Study study) {
    TypedQuery<Long> q = em.createNamedQuery("Activity.countPerStudy",
            Long.class);
    q.setParameter("study", study);
    return q.getSingleResult();
  }

  public List<Activity> activityOnID(int id) {
    Query query = em.createNamedQuery("Activity.findById",
            Activity.class).setParameter("id", id);
    return query.getResultList();
  }

  public Activity lastActivityOnStudy(Study study) {
    TypedQuery<Activity> query = em.createNamedQuery("Activity.findByStudy", Activity.class);
    query.setParameter("study", study);
    query.setMaxResults(1);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      logger.log(Level.SEVERE, "No activity returned for study " + study
              + ", while its creation should always be there!", e);
      return null;
    }
  }

  public void persistActivity(String activity, Study study, User user) {
    Activity a = new Activity();
    a.setActivity(activity);
    a.setStudy(study);
    a.setFlag(FLAG_STUDY);
    a.setUser(user);
    a.setTimestamp(new Date());
    em.persist(a);
  }
  
  public void persistActivity(String activity, Study study, String email){
    TypedQuery<User> userQuery = em.createNamedQuery("User.findByEmail", User.class);
    userQuery.setParameter("email", email);
    User user;
    try{
      user = userQuery.getSingleResult();
    }catch(NoResultException e){
      throw new IllegalArgumentException("No user found with email "+email+" when trying to persist activity for that user.",e);
    }
    persistActivity(activity, study, user);
  }

  /**
   * Gets all activity information.
   * <p>
   * @return
   */
  public List<ActivityDetail> getAllActivityDetail() {
    TypedQuery<ActivityDetail> q = em.createNamedQuery("ActivityDetail.findAll",
            ActivityDetail.class);
    return q.getResultList();
  }

  /**
   * Get all the activities performed on study <i>study</i>.
   * <p>
   * @param study
   * @return
   */
  public List<ActivityDetail> activityDetailOnStudy(Study study) {
    TypedQuery<ActivityDetail> q = em.createNamedQuery(
            "ActivityDetail.findByStudyname", ActivityDetail.class);
    q.setParameter("studyname", study.getName());
    return q.getResultList();
  }

  /**
   * Returns all activity, but paginated. Items from <i>first</i> till
   * <i>first+pageSize</i> are returned.
   * <p>
   * @param first
   * @param pageSize
   * @return
   */
  public List<ActivityDetail> getPaginatedActivityDetail(int first, int pageSize) {
    TypedQuery<ActivityDetail> q = em.createNamedQuery("ActivityDetail.findAll",
            ActivityDetail.class);
    q.setFirstResult(first);
    q.setMaxResults(pageSize);
    return q.getResultList();
  }

  /**
   * Returns all activities on study <i>studyName</i>, but paginated. Items from
   * <i>first</i> till
   * <i>first+pageSize</i> are returned.
   * <p>
   * @param first
   * @param pageSize
   * @param study
   * @return
   */
  public List<ActivityDetail> getPaginatedActivityDetailForStudy(int first,
          int pageSize, Study study) {
    TypedQuery<ActivityDetail> q = em.createNamedQuery(
            "ActivityDetail.findByStudyname", ActivityDetail.class);
    q.setParameter("studyname", study.getName());
    q.setFirstResult(first);
    q.setMaxResults(pageSize);
    return q.getResultList();
  }

  public List<Activity> findAllTeamActivity(String flag) {
    Query query = em.createNamedQuery("Activity.findByFlag",
            Activity.class).setParameter("flag", flag);
    return query.getResultList();
  }
}
