package se.kth.bbc.jobs.jobhistory;

import se.kth.bbc.jobs.model.description.JobDescription;
import java.util.Collection;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 * Facade for management of persistent Execution objects.
 * <p>
 * @author stig
 */
@Stateless
public class ExecutionFacade extends AbstractFacade<Execution> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public ExecutionFacade() {
    super(Execution.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  /**
   * Find all the Execution entries for the given project and type.
   * <p>
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
   * <p>
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
   * Find the execution with given id.
   * <p>
   * @param id
   * @return The found entity, or null if no such exists.
   */
  public Execution findById(Integer id) {
    return em.find(Execution.class, id);
  }

  public Execution create(JobDescription job, Users user, String stdoutPath,
          String stderrPath, Collection<JobInputFile> input) {
    return create(job, user, JobState.INITIALIZING, stdoutPath, stderrPath,
            input);
  }

  public Execution create(JobDescription job, Users user, JobState state,
          String stdoutPath,
          String stderrPath, Collection<JobInputFile> input) {
    //Check if state is ok
    if (state == null) {
      state = JobState.INITIALIZING;
    }
    //Create new object
    Execution exec = new Execution(state, job, user, stdoutPath, stderrPath,
            input);
    //And persist it
    em.persist(exec);
    em.flush();
    return exec;
  }

  public Execution updateState(Execution exec, JobState newState) {
    return update(exec, newState, -1, null, null, null, null,
            null);
  }

  public Execution updateExecutionTime(Execution exec, long executionTime) {
    return update(exec, null, executionTime, null, null, null, null, null);
  }

  public Execution updateOutput(Execution exec,
          Collection<JobOutputFile> outputFiles) {
    return update(exec, null, -1, null, null, null, outputFiles, null);
  }

  public Execution updateStdOutPath(Execution exec, String stdOutPath) {
    return update(exec, null, -1, stdOutPath, null, null, null, null);
  }

  public Execution updateStdErrPath(Execution exec, String stdErrPath) {
    return update(exec, null, -1, null, stdErrPath, null, null, null);
  }

  public Execution updateAppId(Execution exec, String appId) {
    return update(exec, null, -1, null, null, appId, null, null);
  }

  /**
   * Updates all given fields of <i>exec</i> to the given value, unless that
   * value is null for entities, or -1 for integers.
   * <p>
   * @param history
   * @param name
   * @param state
   * @param executionDuration
   * @param stdoutPath
   * @param stderrPath
   * @param appId
   * @param jobInputFileCollection
   * @param jobOutputFileCollection
   */
  private Execution update(
          Execution exec, JobState state, long executionDuration,
          String stdoutPath, String stderrPath, String appId,
          Collection<JobOutputFile> output, Collection<JobInputFile> input) {
    //Find the updated execution object
    Execution obj = em.find(Execution.class, exec.getId());
    if (obj == null) {
      throw new IllegalArgumentException(
              "Unable to find Execution object with id " + exec.getId());
    } else {
      exec = obj;
    }
    if (state != null) {
      exec.setState(state);
    }
    if (executionDuration != -1) {
      exec.setExecutionDuration(executionDuration);
    }
    if (stdoutPath != null) {
      exec.setStdoutPath(stdoutPath);
    }
    if (stderrPath != null) {
      exec.setStderrPath(stderrPath);
    }
    if (appId != null) {
      exec.setAppId(appId);
    }
    if (input != null) {
      exec.setJobInputFileCollection(input);
    }
    if (output != null) {
      exec.setJobOutputFileCollection(output);
    }
    em.merge(exec);
    return exec;
  }

}
