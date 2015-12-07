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
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;
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
  public static final String NEW_PROJECT = " created a new project ";
  public static final String NEW_DATA = " added a new dataset ";
  public static final String SHARED_DATA = " shared dataset ";
  public static final String NEW_MEMBER = " added a member ";
  public static final String NEW_SAMPLE = " added a new sample ";
  public static final String CHANGE_ROLE = " changed the role of ";
  public static final String REMOVED_MEMBER = " removed team member ";
  public static final String REMOVED_SAMPLE = " removed a sample ";
  public static final String REMOVED_FILE = " removed a file ";
  public static final String REMOVED_PROJECT = " removed project ";
  public static final String RAN_JOB = " ran a job ";
  public static final String ADDED_SERVICES = " added new services ";
  public static final String PROJECT_NAME_CHANGED = " changed project name ";
  public static final String PROJECT_DESC_CHANGED
          = " changed project description.";
  public static final String CREATED_JOB = " created a new job ";
  public static final String DELETED_JOB = " deleted a job ";
  public static final String SCHEDULED_JOB = " scheduled a job ";
  // Flag constants
  public static final String FLAG_PROJECT = "PROJECT";
  public static final String FLAG_DATASET = "DATASET";

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

  public long getProjectCount(Project project) {
    TypedQuery<Long> q = em.createNamedQuery("Activity.countPerProject",
            Long.class);
    q.setParameter("project", project);
    return q.getSingleResult();
  }

  public List<Activity> activityOnID(int id) {
    Query query = em.createNamedQuery("Activity.findById",
            Activity.class).setParameter("id", id);
    return query.getResultList();
  }

  public Activity lastActivityOnProject(Project project) {
    TypedQuery<Activity> query = em.createNamedQuery("Activity.findByProject",
            Activity.class);
    query.setParameter("project", project);
    query.setMaxResults(1);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      logger.log(Level.SEVERE, "No activity returned for project " + project
              + ", while its creation should always be there!", e);
      return null;
    }
  }

  public void persistActivity(String activity, Project project, Users user) {
    Activity a = new Activity();
    a.setActivity(activity);
    a.setProject(project);
    a.setFlag(FLAG_PROJECT);
    a.setUser(user);
    a.setTimestamp(new Date());
    em.persist(a);
  }

  public void persistActivity(String activity, Project project, String email) {
    TypedQuery<Users> userQuery = em.createNamedQuery("Users.findByEmail",
            Users.class);
    userQuery.setParameter("email", email);
    Users user;
    try {
      user = userQuery.getSingleResult();
    } catch (NoResultException e) {
      throw new IllegalArgumentException("No user found with email " + email
              + " when trying to persist activity for that user.", e);
    }
    persistActivity(activity, project, user);
  }

  /**
   * Gets all activity information.
   * <p/>
   * @return
   */
  public List<Activity> getAllActivities() {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findAll",
            Activity.class);
    return q.getResultList();
  }

  /**
   * Get all the activities performed on project <i>project</i>.
   * <p/>
   * @param project
   * @return
   */
  public List<Activity> getAllActivityOnProject(Project project) {
    TypedQuery<Activity> q = em.createNamedQuery(
            "Activity.findByProject", Activity.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  /**
   * Get all the activities performed on by user <i>user</i>.
   * <p/>
   * @param user
   * @return
   */
  public List<Activity> getAllActivityByUser(Users user) {
    TypedQuery<Activity> q = em.createNamedQuery(
            "Activity.findByUser", Activity.class);
    q.setParameter("user", user);
    return q.getResultList();
  }

  /**
   * Get all the activities performed on by user <i>user</i>.but paginated.Items
   * from
   * <i>first</i> till
   * <i>first+pageSize</i> are returned.
   * <p/>
   * @param first
   * @param pageSize
   * @param user
   * @return
   */
  public List<Activity> getPaginatedActivityByUser(int first,
          int pageSize, Users user) {
    TypedQuery<Activity> q = em.createNamedQuery(
            "Activity.findByUser", Activity.class);
    q.setParameter("user", user);
    q.setFirstResult(first);
    q.setMaxResults(pageSize);
    return q.getResultList();
  }

  /**
   * Returns all activity, but paginated. Items from <i>first</i> till
   * <i>first+pageSize</i> are returned.
   * <p/>
   * @param first
   * @param pageSize
   * @return
   */
  public List<Activity> getPaginatedActivity(int first, int pageSize) {
    TypedQuery<Activity> q = em.createNamedQuery("Activity.findAll",
            Activity.class);
    q.setFirstResult(first);
    q.setMaxResults(pageSize);
    return q.getResultList();
  }

  /**
   * Returns all activities on project <i>projectName</i>, but paginated. Items
   * from
   * <i>first</i> till
   * <i>first+pageSize</i> are returned.
   * <p/>
   * @param first
   * @param pageSize
   * @param project
   * @return
   */
  public List<Activity> getPaginatedActivityForProject(int first,
          int pageSize, Project project) {
    TypedQuery<Activity> q = em.createNamedQuery(
            "Activity.findByProject", Activity.class);
    q.setParameter("project", project);
    q.setFirstResult(first);
    q.setMaxResults(pageSize);
    return q.getResultList();
  }

  public List<Activity> findAllTeamActivity(String flag) {
    Query query = em.createNamedQuery("Activity.findByFlag",
            Activity.class).setParameter("flag", flag);
    return query.getResultList();
  }
  
  
  /**
   * Get all the activities performed on study <i>studyName</i>.
   * <p>
   * @param studyName
   * @return
   */
  public List<Activity> activityDetailOnStudy(String projName) {
    TypedQuery<Activity> q = em.createNamedQuery(
            "Activity.findByProject", Activity.class);
    q.setParameter("projectname", projName);
    return q.getResultList();
  }

  /**
   * Get all the activities performed on study <i>studyName</i>.
   * <p>
   * @param studyName
   * @param from
   * @param to
   * @return
   */
  public List<Activity> activityDetailOnStudyAudit(String studyName, Date from,
          Date to) {
    String sql =null;
    if(!studyName.isEmpty() && studyName!=null){
      sql = "SELECT * FROM hopsworks.activity WHERE  (created >= '"
            + from + "' AND created <='" + to + "' AND project_id = (SELECT project_id FROM hopsworks.project WHERE projectname='"+studyName +"'))";
    }else {
      sql= "SELECT * FROM hopsworks.activity  WHERE  (created >= '"
            + from + "' AND created <='" + to + "')";
    }
    
    Query query = em.createNativeQuery(sql, Activity.class);

    List<Activity> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }

    return ul;
  }

}
