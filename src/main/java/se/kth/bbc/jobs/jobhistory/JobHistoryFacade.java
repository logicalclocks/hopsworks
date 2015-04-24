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
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.study.StudyFacade;
import se.kth.bbc.study.Study;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.bbc.security.ua.model.User;

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

  @EJB
  private StudyFacade studies;

  public JobHistoryFacade() {
    super(JobHistory.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  /**
   * Find all the JobHistory entries for the given study and type.
   * @param study
   * @param type
   * @return List of JobHistory objects.
   */
  public List<JobHistory> findForStudyByType(Study study, JobType type) {
    TypedQuery<JobHistory> q = em.createNamedQuery(
            "JobHistory.findByStudyAndType", JobHistory.class);
    q.setParameter("type", type);
    q.setParameter("study", study);
    return q.getResultList();
  }

  public void update(Long id, JobState newState) {
    //TODO: check if state is a final one, if so: update execution time
    JobHistory jh = findById(id);
    if (jh.getState() != newState) {
      updateState(id, newState);
    }
  }

  public void update(Long id, JobState newState, long executionTime) {
    JobHistory jh = findById(id);
    updateState(id, newState);
    jh.setExecutionDuration(BigInteger.valueOf(executionTime));
    em.merge(jh);
  }

  public void update(Long id, JobState newState,
          Collection<JobOutputFile> outputFiles) {
    JobHistory jh = findById(id);
    updateState(id, newState);
    Collection<JobOutputFile> output = jh.getJobOutputFileCollection();
    output.addAll(output);
    jh.setJobOutputFileCollection(output);
    em.merge(jh);
  }

  public void update(Long id, Collection<JobOutputFile> extraOutputFiles) {
    JobHistory jh = findById(id);
    Collection<JobOutputFile> output = jh.getJobOutputFileCollection();
    output.addAll(output);
    jh.setJobOutputFileCollection(output);
    em.merge(jh);
  }

  /**
   * Separate method to isolate the state updating transaction. Needed for
   * remote updating.
   * <p>
   * @param jh
   * @param state
   */
  private void updateState(Long id, JobState state) {
    Query q = em.createNativeQuery("UPDATE jobhistory SET state=? WHERE id=?");
    q.setParameter(1, state.name());
    q.setParameter(2, id);
    q.executeUpdate();
  }

  public void updateArgs(Long id, String args) {
    JobHistory jh = findById(id);
    jh.setArgs(args);
    em.merge(jh);
  }

  public JobHistory findById(Long id) {
    if (id == null) {
      throw new NullPointerException();
    }
    TypedQuery<JobHistory> q = em.createNamedQuery("JobHistory.findById",
            JobHistory.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      logger.log(Level.SEVERE, "Tried to look up jobHistory for id " + id
              + ", but no such id could be found.", e);
      throw e;
    }
  }

  public Long create(String jobname, String userEmail, Study study,
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
    jh.setStudy(study);
    jh.setType(type);
    jh.setArgs(args);
    jh.setStdoutPath(stdOutPath);
    jh.setStderrPath(stdErrPath);
    jh.setJobExecutionFileCollection(execFiles);
    jh.setJobInputFileCollection(inputFiles);

    em.persist(jh);
    em.flush();
    return jh.getId();
  }

  public void updateStdOutPath(Long id, String stdOutPath) {
    JobHistory jh = findById(id);
    jh.setStdoutPath(stdOutPath);
    em.merge(jh);
  }

  public void updateStdErrPath(Long id, String stdErrPath) {
    JobHistory jh = findById(id);
    jh.setStderrPath(stdErrPath);
    em.merge(jh);
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
}
