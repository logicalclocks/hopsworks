package se.kth.bbc.jobhistory;

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
public class JobHistoryFacade extends AbstractFacade<JobHistory> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public JobHistoryFacade() {
    super(JobHistory.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public List<JobHistory> findForStudyByType(String studyname, String type){
    TypedQuery<JobHistory> q = em.createNamedQuery("JobHistory.findByStudyAndType", JobHistory.class);
    q.setParameter("type", type);
    q.setParameter("studyname", studyname);
    return q.getResultList();
  }

}
