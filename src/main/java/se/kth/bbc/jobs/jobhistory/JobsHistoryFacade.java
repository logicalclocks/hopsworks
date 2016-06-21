/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;

@Stateless
public class JobsHistoryFacade extends AbstractFacade<JobsHistory>{
    
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private ExecutionInputfilesFacade execInputFiles;
  @EJB
  private FileOperations fileOperations;
    
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

  /**
     * Stores an instance in the database.
     * <p/>
     * @param jobDesc
     * @param executionId 
     * @param appId
     */
  
  public void persist(JobDescription jobDesc, int executionId, String appId){
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
      int inodeSize = (int) inode.getSize();
      String blocks = checkArguments(configuration.getArgs());
      
      this.persist(jobDesc.getId(), inodePid, inodeName, executionId, appId, jobDesc.getJobType().toString(),
              inodeSize, blocks, configuration.getArgs() ,configuration.getMainClass() ,
              configuration.getExecutorMemory(), configuration.getExecutorCores());
  }
   
    public void persist(int jobId, int inodePid, String inodeName, int executionId, String appId, String jobType, int size,
            String inputBlocksInHdfs, String arguments, String className, int initialRequestedMemory, int initialrequestedVcores){
        JobsHistoryPK pk = new JobsHistoryPK(jobId, inodePid, inodeName, executionId);
        JobsHistory exist = em.find(JobsHistory.class, pk);
        if(exist == null){
            JobsHistory file = new JobsHistory(jobId, inodePid, inodeName, executionId, appId, jobType, 
                    size, inputBlocksInHdfs, arguments, className, initialRequestedMemory,
                initialrequestedVcores);
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
     * @param executionId 
     * @param appId 
     * @param duration 
     * @return JobsHistory
     */
    
    public JobsHistory updateJobHistory(int JobId, int inodeId, String inodeName, int executionId, String appId, long duration){
        jPK = new JobsHistoryPK(JobId, inodeId, inodeName, executionId);
        JobsHistory obj = em.find(JobsHistory.class, jPK);
        obj.setAppId(appId);
        obj.setExecutionDuration(duration);
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
      String blocks = "";
      if(arguments.startsWith("hdfs://")){
          try {
              blocks = fileOperations.getFileBlocks(arguments);
          } catch (IOException ex) {
              Logger.getLogger(JobsHistoryFacade.class.getName()).log(Level.SEVERE, null, ex);
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
        String blocks = checkArguments(jobDetails.getInputArgs());
        List<JobsHistory> resultsForAnalysis = searchForVeryHighSimilarity(jobDetails);
        
        if(!resultsForAnalysis.isEmpty()){
            JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis, jobDetails);
            jhDTO.setInputBlocks(blocks);
            jhDTO.addSimilarAppId(resultsForAnalysis);
            jhDTO.setDegreeOfSimilarity("VERY HIGH");
            return jhDTO;
        }
        
        resultsForAnalysis = searchForHighSimilarity(jobDetails);
        
        if(!resultsForAnalysis.isEmpty()){
            JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis, jobDetails);
            jhDTO.setInputBlocks(blocks);
            jhDTO.addSimilarAppId(resultsForAnalysis);
            jhDTO.setDegreeOfSimilarity("HIGH");
            return jhDTO;
        }
            
        resultsForAnalysis = searchForMediumSimilarity(jobDetails);
        
        if(!resultsForAnalysis.isEmpty()){
            JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis, jobDetails);
            jhDTO.setInputBlocks(blocks);
            jhDTO.addSimilarAppId(resultsForAnalysis);
            jhDTO.setDegreeOfSimilarity("MEDIUM");
            return jhDTO;
        }
        
        resultsForAnalysis = searchForLowSimilarity(jobDetails);
        
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
    private List<JobsHistory> searchForVeryHighSimilarity(JobDetailDTO jobDetails){
        TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithVeryHighSimilarity",
            JobsHistory.class);
        q.setParameter("jobType", jobDetails.getJobType());
        q.setParameter("className", jobDetails.getClassName());
        q.setParameter("inodeName", jobDetails.getSelectedJar());
        q.setParameter("arguments", jobDetails.getInputArgs());
        q.setParameter("inputBlocksInHdfs", checkArguments(jobDetails.getInputArgs()));
        
        return q.getResultList();
        
    }
    
    
    // High Similarity -> Same jobType, className, jarFile and arguments
    private List<JobsHistory> searchForHighSimilarity(JobDetailDTO jobDetails){
        TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithHighSimilarity",
            JobsHistory.class);
        q.setParameter("jobType", jobDetails.getJobType());
        q.setParameter("className", jobDetails.getClassName());
        q.setParameter("inodeName", jobDetails.getSelectedJar());
        q.setParameter("arguments", jobDetails.getInputArgs());
        
        return q.getResultList();
        
    }
    
    // Medium Similarity -> Same jobType, className and jarFile
    private List<JobsHistory> searchForMediumSimilarity(JobDetailDTO jobDetails){
        TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithMediumSimilarity",
            JobsHistory.class);
        q.setParameter("jobType", jobDetails.getJobType());
        q.setParameter("className", jobDetails.getClassName());
        q.setParameter("inodeName", jobDetails.getSelectedJar());
        
        return q.getResultList();
    }
    
    // Low Similarity -> Same jobType, className
    private List<JobsHistory> searchForLowSimilarity(JobDetailDTO jobDetails){
        TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findWithLowSimilarity",
            JobsHistory.class);
        q.setParameter("jobType", jobDetails.getJobType());
        q.setParameter("className", jobDetails.getClassName());
        
        return q.getResultList();
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
