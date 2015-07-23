package se.kth.bbc.jobs.jobhistory;

import java.util.Collection;
import java.util.List;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 * Facade for management of persistent JobDescription objects.
 * @author stig
 */
public class JobFacade extends AbstractFacade<JobDescription> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public JobFacade() {
    super(JobDescription.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  /**
   * Find all the JobDescription entries for the given project and type.
   * <p>
   * @param project
   * @param type
   * @return List of JobHistory objects.
   */
  public List<JobDescription> findForProjectByType(Project project, JobType type) {
    TypedQuery<JobDescription> q = em.createNamedQuery("Job.findByProjectAndType", JobDescription.class);
    q.setParameter("type", type);
    q.setParameter("project", project);
    return q.getResultList();
  }
  
  /**
   * Find all the jobs defined in the given project.
   * @param project
   * @return 
   */
  public List<JobDescription> findForProject(Project project){
    TypedQuery<JobDescription> q = em.createNamedQuery("Job.findByProject", JobDescription.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  /**
   * Create a new JobDescription instance.
   * <p>
   * @param name The name of the job.
   * @param user The creator of the job.
   * @param project The project in which this job is defined.
   * @param type The type of the job.
   * @param config The job configuration file.
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) //This seems to ensure that the entity is actually created and can later be found using em.find().
  public JobDescription create(String name, Users user, Project project, JobType type,
           YarnJobConfiguration config) {
    //First: create a job object
    JobDescription job = new JobDescription(type, config, project, user, name);
    //Finally: persist it, getting the assigned id.
    em.persist(job);
    em.flush(); //To get the id.
    return job;
  }
  
  /**
   * Find the JobDescription with given id.
   * @param id
   * @return The found entity or null if no such exists.
   */
  public JobDescription findById(Integer id){
    return em.find(JobDescription.class, id);
  }

}
