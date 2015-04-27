package se.kth.bbc.jobs.jobhistory;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class JobOutputFileFacade extends AbstractFacade<JobOutputFile> {

  @PersistenceContext(unitName = "hopsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public JobOutputFileFacade() {
    super(JobOutputFile.class);
  }

  public void persist(JobOutputFile file) {
    em.persist(file);
  }

  public List<JobOutputFile> findOutputFilesForJobid(Long id) {
    TypedQuery<JobOutputFile> q = em.createNamedQuery(
            "JobOutputFile.findByJobId", JobOutputFile.class);
    q.setParameter("jobId", id);
    return q.getResultList();
  }

  public JobOutputFile findByNameAndJobId(String name, Long id) {
    TypedQuery<JobOutputFile> q = em.createNamedQuery(
            "JobOutputFile.findByNameAndJobId", JobOutputFile.class);
    q.setParameter("name", name);
    q.setParameter("jobId", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException | NonUniqueResultException e) {
      return null;
    }
  }

}
