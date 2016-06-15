/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
      String blocks = "";
      
      String arguments = configuration.getArgs();
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
    
    public JobsHistory updateJobHistory(int JobId, int inodeId, String inodeName, int executionId, String appId, long duration){
        jPK = new JobsHistoryPK(JobId, inodeId, inodeName, executionId);
        JobsHistory obj = em.find(JobsHistory.class, jPK);
        obj.setAppId(appId);
        obj.setExecutionDuration(duration);
        em.merge(obj);
        return obj;   
    }
}
