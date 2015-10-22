package se.kth.bbc.jobs.model.description;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.jobhistory.JobState;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.configuration.JobConfiguration.JobConfigurationFactory;
import se.kth.bbc.jobs.model.configuration.ScheduleDTO;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.rest.JobService;
import se.kth.hopsworks.user.model.Users;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 * Facade for management of persistent JobDescription objects.
 * <p/>
 * @author stig
 */
@Stateless
public class JobDescriptionFacade extends AbstractFacade<JobDescription> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
    private static final Logger logger = Logger.getLogger(JobDescriptionFacade.class.
          getName());

  public JobDescriptionFacade() {
    super(JobDescription.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  /**
   * Find all the jobs in this project with the given type.
   * <p/>
   * @param project
   * @param type
   * @return
   */
  public List<JobDescription> findJobsForProjectAndType(
          Project project, JobType type) {
    TypedQuery<JobDescription> q = em.createNamedQuery(
            "JobDescription.findByProjectAndType",
            JobDescription.class);
    q.setParameter("project", project);
    q.setParameter("type", type);
    return q.getResultList();
  }

  /**
   * Find all the jobs defined in the given project.
   * <p/>
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
   * <p/>
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
  public JobDescription create(Users creator, Project project,
          JobConfiguration config) throws
          IllegalArgumentException, NullPointerException {
    //Argument checking
    if (creator == null || project == null || config == null) {
      throw new NullPointerException(
              "Owner, project and config must be non-null.");
    }
    //First: create a job object
    JobDescription job = new JobDescription(config, project, creator, config.
            getAppName());
    //Finally: persist it, getting the assigned id.
    em.persist(job);
    em.flush(); //To get the id.
    return job;
  }

  /**
   * Find the JobDescription with given id.
   * <p/>
   * @param id
   * @return The found entity or null if no such exists.
   */
  public JobDescription findById(Integer id) {
    return em.find(JobDescription.class, id);
  }
  
  public void removeJob(JobDescription job) throws DatabaseException{
      try{
          JobDescription managedJob = em.find(JobDescription.class, job.getId());
          this.em.remove(managedJob);          
      }catch(SecurityException | IllegalStateException ex){
          throw new DatabaseException("Could not delete job " + job.getName(), ex);
      }
          
  }
  
  public boolean updateJobSchedule(int jobId, ScheduleDTO schedule) throws DatabaseException{
      boolean status=false;
      try{
          JobDescription managedJob = em.find(JobDescription.class, jobId);
          JobConfiguration config=managedJob.getJobConfig();
          config.setSchedule(schedule);
          TypedQuery<JobDescription> q = em.createNamedQuery(
            "JobDescription.updateConfig", JobDescription.class);
          q.setParameter("id", jobId);
          q.setParameter("jobconfig", config);
          int result=q.executeUpdate();
          logger.log(Level.INFO, "Updated entity count ="+result);
          if(result==1){
              status=true;
          }
      }catch(SecurityException | IllegalArgumentException ex){
          throw new DatabaseException("Could not update job  ", ex);
      }
      return status;
  }

  public List<JobDescription> getRunningJobs(Project project) {
    TypedQuery<JobDescription> q = em.createNamedQuery(
            "Execution.findJobsForExecutionInState", JobDescription.class);
    q.setParameter("project", project);
    q.setParameter("stateCollection", JobState.getRunningStates());
    return q.getResultList();
  }

}
