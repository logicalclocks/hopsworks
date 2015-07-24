package se.kth.bbc.jobs.model.description;

import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 * Facade for management of persistent JobDescription objects.
 * <p>
 * @author stig
 */
@Stateless
public class JobDescriptionFacade extends AbstractFacade<JobDescription> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public JobDescriptionFacade() {
    super(JobDescription.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public List<CuneiformJobDescription> findCuneiformJobsForProject(
          Project project) {
    TypedQuery<CuneiformJobDescription> q = em.createNamedQuery(
            "CuneiformJobDescription.findByProject",
            CuneiformJobDescription.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  public List<SparkJobDescription> findSparkJobsForProject(Project project) {
    TypedQuery<SparkJobDescription> q = em.createNamedQuery(
            "SparkJobDescription.findByProject", SparkJobDescription.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  public List<AdamJobDescription> findAdamJobsForProject(Project project) {
    TypedQuery<AdamJobDescription> q = em.createNamedQuery(
            "AdamJobDescription.findByProject", AdamJobDescription.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  public List<YarnJobDescription> findYarnJobsForProject(Project project) {
    TypedQuery<YarnJobDescription> q = em.createNamedQuery(
            "YarnJobDescription.findByProject", YarnJobDescription.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  /**
   * Find all the jobs defined in the given project.
   * <p>
   * @param project
   * @return
   */
  public List<JobDescription> findForProject(Project project) {
    TypedQuery<JobDescription> q = em.createNamedQuery(
            "JobDescription.findByProject", JobDescription.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  /**
   * Create a new JobDescription instance.
   * <p>
   * @param name The name of the job.
   * @param creator The creator of the job.
   * @param project The project in which this job is defined.
   * @param config The job configuration file.
   * @return
   * @throws IllegalArgumentException If the JobConfiguration object is not
   * parseable to a known class.
   * @throws NullPointerException If any of the arguments user, project or
   * config are null.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) //This seems to ensure that the entity is actually created and can later be found using em.find().
  public JobDescription<? extends JobConfiguration> create(String name,
          Users creator, Project project, JobConfiguration config) throws
          IllegalArgumentException, NullPointerException {
    //Argument checking
    if (creator == null || project == null || config == null) {
      throw new NullPointerException(
              "Owner, project and config must be non-null.");
    }
    //First: create a job object
    JobDescription job = JobDescriptionFactory.
            getNewJobDescription(config, name, creator, project);
    //Finally: persist it, getting the assigned id.
    em.persist(job);
    em.flush(); //To get the id.
    return job;
  }

  /**
   * Find the JobDescription with given id.
   * <p>
   * @param id
   * @return The found entity or null if no such exists.
   */
  public JobDescription findById(Integer id) {
    return em.find(JobDescription.class, id);
  }

}
