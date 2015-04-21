package se.kth.bbc.activity;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author roshan
 */
@Stateless
public class ActivityFacade extends AbstractFacade<UserActivity> {

  // String constants
  public static final String NEW_STUDY = " created new study ";
  public static final String NEW_DATA = " added a new dataset ";
  public static final String NEW_MEMBER = " added new member ";
  public static final String NEW_SAMPLE = " added a new sample ";
  public static final String CHANGE_ROLE = " changed role of ";
  public static final String REMOVED_MEMBER = " removed team member ";
  public static final String REMOVED_SAMPLE = " removed a sample ";
  public static final String REMOVED_FILE = " removed a file ";
  public static final String REMOVED_STUDY = " removed study ";
  public static final String RAN_JOB = " ran a job ";
  // Flag constants
  public static final String FLAG_STUDY = "STUDY";

  @PersistenceContext(unitName = "hopsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ActivityFacade() {
    super(UserActivity.class);
  }

  public void persistActivity(UserActivity activity) {
    em.persist(activity);
  }

  public void removetActivity(UserActivity activity) {
    em.remove(activity);
  }

  public long getTotalCount() {
    TypedQuery<Long> q = em.
            createNamedQuery("UserActivity.countAll", Long.class);
    return q.getSingleResult();
  }

  public long getStudyCount(String studyName) {
    TypedQuery<Long> q = em.createNamedQuery("UserActivity.countStudy",
            Long.class);
    q.setParameter("studyName", studyName);
    return q.getSingleResult();
  }

  public List<UserActivity> activityOnID(int id) {
    Query query = em.createNamedQuery("UserActivity.findById",
            UserActivity.class).setParameter("id", id);
    return query.getResultList();
  }

  public List<UserActivity> lastActivityOnStudy(String name) {
    Query query = em.createNativeQuery(
            "SELECT * FROM activity WHERE activity_on=? ORDER BY created DESC LIMIT 1",
            UserActivity.class).setParameter(1, name);
    return query.getResultList();
  }

  public void persistActivity(String activity, String study, String user) {
    UserActivity a = new UserActivity();
    a.setActivity(activity);
    a.setActivityOn(study);
    a.setFlag(FLAG_STUDY);
    a.setPerformedBy(user);
    a.setTimestamp(new Date());
    em.persist(a);
  }
}
