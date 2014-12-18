package se.kth.bbc.jobs.jobhistory;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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

  public List<JobHistory> findForStudyByType(String studyname, String type) {
    TypedQuery<JobHistory> q = em.createNamedQuery(
            "JobHistory.findByStudyAndType", JobHistory.class);
    q.setParameter("type", type);
    q.setParameter("studyname", studyname);
    return q.getResultList();
  }

  //TODO: check validity of new state
  public void update(Long id, String newState) {
    //TODO: check if state is a final one, if so: update execution time
    JobHistory jh = findById(id);
    jh.setState(newState);
    em.merge(jh);
  }

  public void update(Long id, String newState,
          Collection<JobOutputFile> outputFiles) {
    //TODO: check if state is a final one, if so: update execution time
    JobHistory jh = findById(id);
    jh.setState(newState);
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

  public JobHistory findById(Long id) {
    TypedQuery<JobHistory> q = em.createNamedQuery("JobHistory.findById",
            JobHistory.class);
    q.setParameter("id", id);
    return q.getSingleResult();
  }
  
  public Long create(String jobname, String userEmail, String studyname, String type,
          String args, String state, String stdOutPath, String stdErrPath, 
          Collection<JobExecutionFile> execFiles, Collection<JobInputFile> inputFiles){
    Username user = users.findByEmail(userEmail);
    TrackStudy study = studies.findByName(studyname);
    Date submission = new Date();
    if(state == null || state.isEmpty()){
      state = JobHistory.STATE_NEW;
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

}
