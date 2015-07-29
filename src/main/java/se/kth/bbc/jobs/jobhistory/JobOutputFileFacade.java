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

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public JobOutputFileFacade() {
    super(JobOutputFile.class);
  }

  public void create(Execution execution, String name, String path) {
    if (execution == null) {
      throw new NullPointerException(
              "Cannot create an OutputFile for null Execution.");
    }
    JobOutputFile file = new JobOutputFile(execution.getId(), name);
    file.setPath(path);
    em.persist(file);
  }

  public List<JobOutputFile> findOutputFilesForExecutionId(Long id) {
    TypedQuery<JobOutputFile> q = em.createNamedQuery(
            "JobOutputFile.findByExecutionId", JobOutputFile.class);
    q.setParameter("jobId", id);
    return q.getResultList();
  }

  public JobOutputFile findByNameAndExecutionId(String name, Long id) {
    TypedQuery<JobOutputFile> q = em.createNamedQuery(
            "JobOutputFile.findByNameAndExecutionId", JobOutputFile.class);
    q.setParameter("name", name);
    q.setParameter("jobId", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException | NonUniqueResultException e) {
      return null;
    }
  }

}
