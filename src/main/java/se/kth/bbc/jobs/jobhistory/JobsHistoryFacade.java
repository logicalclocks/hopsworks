package se.kth.bbc.jobs.jobhistory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.hopsworks.user.model.Users;

@Stateless
public class JobsHistoryFacade extends AbstractFacade<JobsHistory>{
    
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private ExecutionInputfilesFacade execInputFiles;
  @EJB
  private FileOperations fileOperations;
  @EJB
  private ProjectFacade projectFacade;
    
  private static final Logger logger = Logger.getLogger(JobsHistoryFacade.class.
      getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  private JobsHistoryPK jPK;


  public JobsHistoryFacade() {
    super(JobsHistory.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public JobsHistory findByAppId(String appId){
      TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findByAppId",
            JobsHistory.class);
            q.setParameter("appId", appId);
            
            return q.getSingleResult();
  }
  
  /**
     * Stores an instance in the database.
     * <p/>
     * @param user
     * @param jobDesc
     * @param executionId 
     * @param appId
     */
  
  public void persist(Users user, JobDescription jobDesc, int executionId, String appId){
      SparkJobConfiguration configuration = (SparkJobConfiguration) jobDesc.getJobConfig();
      String inodePath = configuration.getJarPath();
      String patternString = "hdfs://(.*)\\s";
      Pattern p = Pattern.compile(patternString);
      Matcher m = p.matcher(inodePath);
      String[] parts = inodePath.split("/");
      String pathOfInode = inodePath.replace("hdfs://" + parts[2], "");
      
      Inode inode = inodeFacade.getInodeAtPath(pathOfInode);
      int inodePid = inode.getInodePK().getParentId();
      String inodeName = inode.getInodePK().getName();
      String blocks = checkArguments(configuration.getArgs());
      
      this.persist(jobDesc.getId(), inodePid, inodeName, executionId, appId, jobDesc,
              blocks, configuration, user.getEmail());
  }
   
    public void persist(int jobId, int inodePid, String inodeName, int executionId, String appId, JobDescription jobDesc, String inputBlocksInHdfs,
                        SparkJobConfiguration configuration, String userEmail){
            JobsHistoryPK pk = new JobsHistoryPK(jobId, inodePid, inodeName, executionId);
            JobsHistory exist = em.find(JobsHistory.class, pk);
            if(exist == null){
                JobsHistory file = new JobsHistory(jobId, inodePid, inodeName, executionId, appId, jobDesc, 
                            inputBlocksInHdfs, configuration, userEmail);
                em.persist(file);
                em.flush();
            }
    }
   
    /**
     * Updates a JobHistory instance with the duration and the application Id
     * <p/>
     * @param JobId 
     * @param inodeId  
     * @param inodeName 
     * @param exec
     * @param duration
     * @return JobsHistory
     */
    
    public JobsHistory updateJobHistory(int JobId, int inodeId, String inodeName, Execution exec, long duration){
        jPK = new JobsHistoryPK(JobId, inodeId, inodeName, exec.getId());
        JobsHistory obj = em.find(JobsHistory.class, jPK);
        obj.setAppId(exec.getAppId());
        obj.setExecutionDuration(duration);
        obj.setState(exec.getState());
        obj.setFinalStatus(exec.getFinalStatus());
        em.merge(obj);
        return obj;   
    }
    
    /**
     * Check the input arguments of a job. If it is a file then the method returns the number of blocks that the file consists of.
     * Otherwise the method returns 0 block files.
     * <p/>
     * @param arguments
     * @return Number of Blocks
     */
    
    private String checkArguments(String arguments){
      String blocks = "0";
      if(arguments.startsWith("hdfs://")){
          try {
              blocks = fileOperations.getFileBlocks(arguments);
          } catch (IOException ex) {
              Logger.getLogger(JobsHistoryFacade.class.getName()).log(Level.SEVERE, "Failed to find file at HDFS.", ex);
          }
      }
      else{
          blocks = "0";
      }
      
      return blocks;
    }
    
    /**
     * Given the initial arguments for the user, this method tries to find similar jobs in the history.
     * @param jobDetails
     * @return JobHeuristicDTO
     */
    public JobHeuristicDTO searchHeuristicRusults(JobDetailDTO jobDetails){
        Project project = projectFacade.find(jobDetails.getProjectId());
        String projectName = project.getName();
        String userEmail = project.getOwner().getEmail();
        
        int blocks = Integer.parseInt(checkArguments(jobDetails.getInputArgs()));
        List<JobsHistory> resultsForAnalysis = searchForVeryHighSimilarity(jobDetails, userEmail, projectName);
        
        if(!resultsForAnalysis.isEmpty()){
            JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis, jobDetails);
            jhDTO.setInputBlocks(blocks);
            jhDTO.addSimilarAppId(resultsForAnalysis);
            jhDTO.setDegreeOfSimilarity("VERY HIGH");
            return jhDTO;
        }
        
        resultsForAnalysis = searchForHighSimilarity(jobDetails, userEmail, projectName);
        
        if(!resultsForAnalysis.isEmpty()){
            JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis, jobDetails);
            jhDTO.setInputBlocks(blocks);
            jhDTO.addSimilarAppId(resultsForAnalysis);
            jhDTO.setDegreeOfSimilarity("HIGH");
            return jhDTO;
        }
            
        resultsForAnalysis = searchForMediumSimilarity(jobDetails, userEmail, projectName);
        
        if(!resultsForAnalysis.isEmpty()){
            JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis, jobDetails);
            jhDTO.setInputBlocks(blocks);
            jhDTO.addSimilarAppId(resultsForAnalysis);
            jhDTO.setDegreeOfSimilarity("MEDIUM");
            return jhDTO;
        }
        
        resultsForAnalysis = searchForLowSimilarity(jobDetails, userEmail, projectName);
        
        if(!resultsForAnalysis.isEmpty()){
            JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis, jobDetails);
            jhDTO.setInputBlocks(blocks);
            jhDTO.addSimilarAppId(resultsForAnalysis);
            jhDTO.setDegreeOfSimilarity("LOW");
            return jhDTO;
        }
        else{
            return new JobHeuristicDTO(0, "There are no results", "none","NONE", blocks);
        }
    }
    
    // Very High Similarity -> Same jobType, className, jarFile, arguments and blocks
    // If Filter is true then we search by the above attributes + same job of a user (user + project + job)
    private List<JobsHistory> searchForVeryHighSimilarity(JobDetailDTO jobDetails, String userEmail, String projectName){
        if(jobDetails.isFilter()){
            TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithVeryHighSimilarityFilter",
            JobsHistory.class);
            q.setParameter("jobType", jobDetails.getJobType());
            q.setParameter("className", jobDetails.getClassName());
            q.setParameter("inodeName", jobDetails.getSelectedJar());
            q.setParameter("arguments", jobDetails.getInputArgs());
            q.setParameter("inputBlocksInHdfs", checkArguments(jobDetails.getInputArgs()));
            q.setParameter("projectName", projectName);
            q.setParameter("jobName", jobDetails.getJobName());
            q.setParameter("userEmail", userEmail);
            q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);
            
            return q.getResultList();
        }
        else{
            TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithVeryHighSimilarity",
                JobsHistory.class);
            q.setParameter("jobType", jobDetails.getJobType());
            q.setParameter("className", jobDetails.getClassName());
            q.setParameter("inodeName", jobDetails.getSelectedJar());
            q.setParameter("arguments", jobDetails.getInputArgs());
            q.setParameter("inputBlocksInHdfs", checkArguments(jobDetails.getInputArgs()));
            q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);
        
        return q.getResultList();
        }
    }
    
    
    // High Similarity -> Same jobType, className, jarFile and arguments
    // If Filter is true then we search by the above attributes + same job of a user (user + project + job)
    private List<JobsHistory> searchForHighSimilarity(JobDetailDTO jobDetails, String userEmail, String projectName){
        if(jobDetails.isFilter()){
            TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithHighSimilarityFilter",
                JobsHistory.class);
            q.setParameter("jobType", jobDetails.getJobType());
            q.setParameter("className", jobDetails.getClassName());
            q.setParameter("inodeName", jobDetails.getSelectedJar());
            q.setParameter("arguments", jobDetails.getInputArgs());
            q.setParameter("projectName", projectName);
            q.setParameter("jobName", jobDetails.getJobName());
            q.setParameter("userEmail", userEmail);
            q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);
        
        return q.getResultList();
        }
        
        else{
            TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithHighSimilarity",
                JobsHistory.class);
            q.setParameter("jobType", jobDetails.getJobType());
            q.setParameter("className", jobDetails.getClassName());
            q.setParameter("inodeName", jobDetails.getSelectedJar());
            q.setParameter("arguments", jobDetails.getInputArgs());
            q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);
        
        return q.getResultList();
        }
    }
    
    // Medium Similarity -> Same jobType, className and jarFile
    // If Filter is true then we search by the above attributes + same job of a user (user + project + job)
    private List<JobsHistory> searchForMediumSimilarity(JobDetailDTO jobDetails, String userEmail, String projectName){
        if(jobDetails.isFilter()){
            TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithMediumSimilarityFilter",
                JobsHistory.class);
            q.setParameter("jobType", jobDetails.getJobType());
            q.setParameter("className", jobDetails.getClassName());
            q.setParameter("inodeName", jobDetails.getSelectedJar());
            q.setParameter("projectName", projectName);
            q.setParameter("jobName", jobDetails.getJobName());
            q.setParameter("userEmail", userEmail);
            q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);
        
            return q.getResultList();
        }
        else{
            TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithMediumSimilarity",
                JobsHistory.class);
            q.setParameter("jobType", jobDetails.getJobType());
            q.setParameter("className", jobDetails.getClassName());
            q.setParameter("inodeName", jobDetails.getSelectedJar());
            q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);
        
            return q.getResultList();
        }
    }
    
    // Low Similarity -> Same jobType, className
    // If Filter is true then we search by the above attributes + same job of a user (user + project + job)
    private List<JobsHistory> searchForLowSimilarity(JobDetailDTO jobDetails, String userEmail, String projectName){
        if(jobDetails.isFilter()){
            TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithLowSimilarityFilter",
                JobsHistory.class);
            q.setParameter("jobType", jobDetails.getJobType());
            q.setParameter("className", jobDetails.getClassName());
            q.setParameter("projectName", projectName);
            q.setParameter("jobName", jobDetails.getJobName());
            q.setParameter("userEmail", userEmail);
            q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);
        
            return q.getResultList();
        }
        else{
            TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithLowSimilarity",
                JobsHistory.class);
            q.setParameter("jobType", jobDetails.getJobType());
            q.setParameter("className", jobDetails.getClassName());
            q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);
        
            return q.getResultList();
        }
    }
    
    private JobHeuristicDTO analysisOfHeuristicResults(List<JobsHistory> resultsForAnalysis, JobDetailDTO jobDetails){
        String estimatedTime = estimateComletionTime(resultsForAnalysis);
        int numberOfResults = resultsForAnalysis.size();
        String message =  "Analysis of the results.";
        
        return new JobHeuristicDTO(numberOfResults, message, estimatedTime, 
                jobDetails.getProjectId(), jobDetails.getJobName(), jobDetails.getJobType());    
    }
    
    /**
     * Calculates the average of completion time for a bunch of Heuristic results
     * @param resultsForAnalysis
     * @return 
     */
    private String estimateComletionTime(List<JobsHistory> resultsForAnalysis){
        long milliseconds = 0;
        long avegareMs;
        Iterator<JobsHistory> itr = resultsForAnalysis.iterator();
        
        while(itr.hasNext()) {
         JobsHistory element = itr.next();
         milliseconds = milliseconds + element.getExecutionDuration();
      }
        avegareMs = milliseconds / resultsForAnalysis.size();
        
        String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(avegareMs),
            TimeUnit.MILLISECONDS.toMinutes(avegareMs) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(avegareMs)),
            TimeUnit.MILLISECONDS.toSeconds(avegareMs) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(avegareMs)));
    
        return hms;
    }
    
}
