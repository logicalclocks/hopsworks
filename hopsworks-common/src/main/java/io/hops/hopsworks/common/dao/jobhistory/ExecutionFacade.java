package io.hops.hopsworks.common.dao.jobhistory;

import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobs.JobInputFile;
import io.hops.hopsworks.common.dao.jobs.JobOutputFile;
import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import java.util.Arrays;

/**
 * Facade for management of persistent Execution objects.
 */
@Stateless
public class ExecutionFacade extends AbstractFacade<Execution> {

  private static final Logger logger = Logger.getLogger(ExecutionFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public ExecutionFacade() {
    super(Execution.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  private HashMap<Integer, Execution> executions = new HashMap<>();

  /**
   * Find all the Execution entries for the given project and type.
   * <p/>
   * @param project
   * @param type
   * @return List of JobHistory objects.
   * @throws IllegalArgumentException If the given JobType is not supported.
   */
  public List<Execution> findForProjectByType(Project project, JobType type)
          throws IllegalArgumentException {
    TypedQuery<Execution> q = em.createNamedQuery(
            "Execution.findByProjectAndType", Execution.class);
    q.setParameter("type", type);
    q.setParameter("project", project);
    return q.getResultList();
  }

  /**
   * Get all the executions for a given JobDescription.
   * <p/>
   * @param job
   * @return
   */
  public List<Execution> findForJob(JobDescription job) {
    TypedQuery<Execution> q = em.createNamedQuery("Execution.findByJob",
            Execution.class);
    q.setParameter("job", job);
    return q.getResultList();
  }

  /**
   * Get an execution for application id.
   * <p/>
   * @param appId
   * @return
   */
  public Execution findByAppId(String appId) {
    try {
      return em.createNamedQuery("Execution.findByAppId",
              Execution.class).setParameter("appId", appId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<Execution> findbyProjectAndJobId(Project project, int jobId) {
    TypedQuery<Execution> q = em.createNamedQuery(
            "Execution.findByProjectAndJobId",
            Execution.class);
    q.setParameter("jobid", jobId);
    q.setParameter("project", project);
    return q.getResultList();
  }

  /**
   * Get all executions that are not in a final state.
   * <p/>
   * @return
   */
  public List<Execution> findAllNotFinished() {
    try {
      return em.createNamedQuery("Execution.findByStates",
          Execution.class).setParameter("states", Arrays.asList(JobState.RUNNING, JobState.ACCEPTED,
              JobState.AGGREGATING_LOGS, JobState.INITIALIZING, JobState.NEW, JobState.NEW_SAVING,
              JobState.STARTING_APP_MASTER, JobState.SUBMITTED)).getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Find the execution with given id.
   * <p/>
   * @param id
   * @return The found entity, or null if no such exists.
   */
  public Execution findById(Integer id) {
    return em.find(Execution.class, id);
  }

  public Execution create(JobDescription job, Users user, String stdoutPath,
          String stderrPath, Collection<JobInputFile> input,
          JobFinalStatus finalStatus, float progress, String hdfsUser) {
    return create(job, user, JobState.INITIALIZING, stdoutPath, stderrPath,
            input, finalStatus, progress, hdfsUser);
  }

  public Execution create(JobDescription job, Users user, JobState state,
          String stdoutPath,
          String stderrPath, Collection<JobInputFile> input,
          JobFinalStatus finalStatus, float progress, String hdfsUser) {
    //Check if state is ok
    if (state == null) {
      state = JobState.INITIALIZING;
    }
    if (finalStatus == null) {
      finalStatus = JobFinalStatus.UNDEFINED;
    }
    //Create new object
    Execution exec = new Execution(state, job, user, stdoutPath, stderrPath,
            input, finalStatus, progress, hdfsUser);
    //And persist it
    em.persist(exec);
    em.flush();
    return exec;
  }

  public Execution updateState(Execution exec, JobState newState) {
    exec = getExecution(exec);
    exec.setState(newState);
    merge(exec);
    return exec;
  }

  public Execution updateFinalStatus(Execution exec, JobFinalStatus finalStatus) {
    exec = getExecution(exec);
    exec.setFinalStatus(finalStatus);
    merge(exec);
    return exec;
  }

  public Execution updateProgress(Execution exec, float progress) {
    exec = getExecution(exec);
    exec.setProgress(progress);
    merge(exec);
    return exec;
  }

  public Execution updateExecutionStart(Execution exec, long executionStart) {
    exec = getExecution(exec);
    exec.setExecutionStart(executionStart);
    merge(exec);
    return exec;
  }

  public Execution updateExecutionStop(Execution exec, long executionStop) {
    exec = getExecution(exec);
    exec.setExecutionStop(executionStop);
    merge(exec);
    return exec;
  }
  
  public Execution updateOutput(Execution exec,
          Collection<JobOutputFile> outputFiles) {
    exec = getExecution(exec);
    exec.setJobOutputFileCollection(outputFiles);
    merge(exec);
    return exec;
  }

  public Execution updateStdOutPath(Execution exec, String stdOutPath) {
    exec = getExecution(exec);
    exec.setStdoutPath(stdOutPath);
    merge(exec);
    return exec;
  }

  public Execution updateStdErrPath(Execution exec, String stdErrPath) {
    exec = getExecution(exec);
    exec.setStderrPath(stdErrPath);
    merge(exec);
    return exec;
  }

  public Execution updateAppId(Execution exec, String appId) {
    exec = getExecution(exec);
    exec.setAppId(appId);
    merge(exec);
    return exec;
  }

  public Execution updateFilesToRemove(Execution exec, List<String> filesToRemove) {
    exec = getExecution(exec);
    exec.setFilesToRemove(filesToRemove);
    merge(exec);
    return exec;
  }
  
  private Execution getExecution(Execution exec){
    //Find the updated execution object
    Execution obj = em.find(Execution.class, exec.getId());
    int count = 0;
    while (obj == null && count < 10) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        logger.log(Level.SEVERE, null, ex);
      }
      logger.info("Trying to get the Execution Object");
      obj = em.find(Execution.class, exec.getId());
      count++;
    }
    if (obj == null) {
      throw new IllegalArgumentException(
              "Unable to find Execution object with id " + exec.getId());
    }
    return obj;
  }
  
  private void merge(Execution exec){
    em.merge(exec);
    executions.put(exec.getJob().getId(), exec);
  }

  public Execution getExecution(int id) {
    try {
      return executions.get(id);
    } catch (Exception e) {
      return null;
    }
  }

}
