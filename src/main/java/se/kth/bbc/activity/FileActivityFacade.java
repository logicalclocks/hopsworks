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
public class FileActivityFacade extends AbstractFacade<Activity> {

  private static final Logger logger = Logger.getLogger(FileActivityFacade.class.
          getName());

//  public static final String NEW_PROJECT = " created a new project named ";

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public FileActivityFacade() {
    super(Activity.class);
  }

  public void persistActivity(FileActivity activity) {
    em.persist(activity);
  }

  public void removeActivity(FileActivity activity) {
    em.remove(activity);
  }


  public List<Activity> activityOnID(int id) {
    Query query = em.createNamedQuery("FileActivity.findById",
            Activity.class).setParameter("id", id);
    return query.getResultList();
  }
  
}
