package se.kth.bbc.activity;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class ActivityDetailFacade extends AbstractFacade<ActivityDetail> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ActivityDetailFacade() {
    super(ActivityDetail.class);
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
   * Get all the activities performed on study <i>studyName</i>.
   * <p>
   * @param studyName
   * @return
   */
  public List<ActivityDetail> activityDetailOnStudy(String studyName) {
    TypedQuery<ActivityDetail> q = em.createNamedQuery(
            "ActivityDetail.findByStudyname", ActivityDetail.class);
    q.setParameter("studyname", studyName);
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
   * @param studyName
   * @return
   */
  public List<ActivityDetail> getPaginatedActivityDetailForStudy(int first,
          int pageSize, String studyName) {
    TypedQuery<ActivityDetail> q = em.createNamedQuery(
            "ActivityDetail.findByStudyname", ActivityDetail.class);
    q.setParameter("studyname", studyName);
    q.setFirstResult(first);
    q.setMaxResults(pageSize);
    return q.getResultList();
  }

}
