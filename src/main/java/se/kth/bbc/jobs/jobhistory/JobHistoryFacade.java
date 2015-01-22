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
import javax.persistence.TypedQuery;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import se.kth.bbc.study.StudyFacade;
import se.kth.bbc.study.TrackStudy;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.kthfsdashboard.user.UserFacade;
import se.kth.kthfsdashboard.user.Username;

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
  private UserFacade users;

  @EJB
  private StudyFacade studies;

  public JobHistoryFacade() {
    super(JobHistory.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public List<JobHistory> findForStudyByType(String studyname, JobType type) {
    TypedQuery<JobHistory> q = em.createNamedQuery(
            "JobHistory.findByStudyAndType", JobHistory.class);
    q.setParameter("type", type);
    q.setParameter("studyname", studyname);
    return q.getResultList();
  }

  //TODO: check validity of new state
  public void update(Long id, JobState newState) {
    //TODO: check if state is a final one, if so: update execution time
    JobHistory jh = findById(id);
    if (jh.getState() != newState) {
      jh.setState(newState);
      em.merge(jh);
      em.flush();
      publishStateChange(jh);
    }
  }

  public void update(Long id, JobState newState, long executionTime) {
    JobHistory jh = findById(id);
    JobState oldstate = jh.getState();
    jh.setState(newState);
    jh.setExecutionDuration(BigInteger.valueOf(executionTime));
    em.merge(jh);
    if(oldstate != newState)
    publishStateChange(jh);
  }

  public void update(Long id, JobState newState,
          Collection<JobOutputFile> outputFiles) {
    //TODO: check if state is a final one, if so: update execution time
    JobHistory jh = findById(id);
    JobState oldstate = jh.getState();
    jh.setState(newState);
    Collection<JobOutputFile> output = jh.getJobOutputFileCollection();
    output.addAll(output);
    jh.setJobOutputFileCollection(output);
    em.merge(jh);
    if(oldstate != newState)
    publishStateChange(jh);
  }

  public void update(Long id, Collection<JobOutputFile> extraOutputFiles) {
    JobHistory jh = findById(id);
    Collection<JobOutputFile> output = jh.getJobOutputFileCollection();
    output.addAll(output);
    jh.setJobOutputFileCollection(output);
    em.merge(jh);
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

  public Long create(String jobname, String userEmail, String studyname,
          JobType type,
          String args, JobState state, String stdOutPath, String stdErrPath,
          Collection<JobExecutionFile> execFiles,
          Collection<JobInputFile> inputFiles) {
    Username user = users.findByEmail(userEmail);
    TrackStudy study = studies.findByName(studyname);
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
    publishStateChange(jh);
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

  /**
   * Publish the state change to Primefaces Push.
   * TODO: should this be somewhere else? Separation of concerns?
   */
  private void publishStateChange(JobHistory jh) {
    EventBus eventBus = EventBusFactory.getDefault().eventBus();
    eventBus.publish("/" + jh.getStudy().getName() + "/" + jh.getType(), jh.
            getId().toString());

  }

}
