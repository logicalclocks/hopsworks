package se.kth.bbc.jobs.jobhistory;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class JobHistoryFacade extends AbstractFacade<JobHistory> {

  private static final Logger logger = Logger.getLogger(JobHistoryFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private UserManager users;

  public JobHistoryFacade() {
    super(JobHistory.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  /**
   * Find all the JobHistory entries for the given project and type.
   * <p>
   * @param project
   * @param type
   * @return List of JobHistory objects.
   */
  public List<JobHistory> findForProjectByType(Project project, JobType type) {
    TypedQuery<JobHistory> q = em.createNamedQuery(
            "JobHistory.findByProjectAndType", JobHistory.class);
    q.setParameter("type", type);
    q.setParameter("project", project);
    return q.getResultList();
  }

  public void update(JobHistory history, JobState newState) {
    //TODO: check if state is a final one, if so: update execution time
    update(history, null, newState, -1, null, null, null, null, null);
  }

  public void update(JobHistory history, JobState newState, long executionTime) {
    update(history, null, newState, executionTime, null, null, null, null, null);
  }

  public void update(JobHistory history, JobState newState,
          Collection<JobOutputFile> outputFiles) {
    update(history, null, newState, -1, null, null, null, null, outputFiles);
  }

  public void update(JobHistory history,
          Collection<JobOutputFile> extraOutputFiles) {
    update(history, null, null, -1, null, null, null, null, extraOutputFiles);
  }

  public void updateArgs(JobHistory history, String args) {
    update(history, null, null, -1, args, null, null, null, null);
  }

  public JobHistory findById(Long id) {
    return em.find(JobHistory.class, id);
  }

  public JobHistory create(String jobname, String userEmail, Project project,
          JobType type,
          String args, JobState state, String stdOutPath, String stdErrPath,
          Collection<JobExecutionFile> execFiles,
          Collection<JobInputFile> inputFiles) {
    User user = users.findByEmail(userEmail);
    Date submission = new Date(); //now
    if (state == null) {
      state = JobState.INITIALIZING;
    }

    JobHistory jh = new JobHistory(submission, state);
    jh.setName(jobname);
    jh.setUser(user);
    jh.setProject(project);
    jh.setType(type);
    jh.setArgs(args);
    jh.setStdoutPath(stdOutPath);
    jh.setStderrPath(stdErrPath);
    jh.setJobExecutionFileCollection(execFiles);
    jh.setJobInputFileCollection(inputFiles);

    em.persist(jh);
    em.flush();
    return jh;
  }

  public void updateStdOutPath(JobHistory history, String stdOutPath) {
    update(history, null, null, -1, null, stdOutPath, null, null, null);
  }

  public void updateStdErrPath(JobHistory history, String stdErrPath) {
    update(history, null, null, -1, null, null, stdErrPath, null, null);
  }

  public JobState getState(Long jobId) {
    TypedQuery<JobState> q = em.createNamedQuery("JobHistory.findStateForId",
            JobState.class);
    q.setParameter("id", jobId);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void persist(JobOutputFile jof) {
    em.persist(jof);
  }

  /**
   * Updates all given fields of <i>history</i> to the given value, unless that
   * value is null for entities, or -1 for integers.
   * <p>
   * @param history
   * @param name
   * @param state
   * @param executionDuration
   * @param args
   * @param stdoutPath
   * @param stderrPath
   * @param jobInputFileCollection
   * @param jobOutputFileCollection
   */
  public void update(JobHistory history, String name, JobState state,
          long executionDuration, String args, String stdoutPath,
          String stderrPath, Collection<JobInputFile> jobInputFileCollection,
          Collection<JobOutputFile> jobOutputFileCollection) {
    if (name != null) {
      history.setName(name);
    }
    if (state != null) {
      history.setState(state);
    }
    if (executionDuration != -1) {
      history.setExecutionDuration(BigInteger.valueOf(executionDuration));
    }
    if (args != null) {
      history.setArgs(args);
    }
    if (stdoutPath != null) {
      history.setStdoutPath(stdoutPath);
    }
    if (stderrPath != null) {
      history.setStderrPath(stderrPath);
    }
    if (jobInputFileCollection != null) {
      history.setJobInputFileCollection(jobInputFileCollection);
    }
    if (jobOutputFileCollection != null) {
      history.setJobOutputFileCollection(jobOutputFileCollection);
    }
    em.merge(history);
  }
  
  public void refresh(JobHistory object){
    em.refresh(object);
  }
}
