package se.kth.hopsworks.controller;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.JobsHistoryFacade;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.hopsworks.user.model.Users;

/**
 * Takes care of booting the execution of a job.
 * <p/>
 * @author stig
 */
@Stateless
public class ExecutionController {

  //Controllers
  @EJB
  private SparkController sparkController;
  @EJB
  private AdamController adamController;
  @EJB
  private FlinkController flinkController;
  @EJB
  private InodeFacade inodes;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JobsHistoryFacade jobHistoryFac;
  
  final Logger logger = LoggerFactory.getLogger(ExecutionController.class);

  public Execution start(JobDescription job, Users user) throws IOException {
    Execution exec = null;

    switch (job.getJobType()) {
      case ADAM:
        exec = adamController.startJob(job, user);
//        if (exec == null) {
//          throw new IllegalArgumentException("Problem getting execution object for: " + job.
//              getJobType());
//        }
//        int execId = exec.getId();
//        AdamJobConfiguration adamConfig = (AdamJobConfiguration) job.getJobConfig();
//        String path = adamConfig.getJarPath();
//        String[] parts = path.split("/");
//        String pathOfInode = path.replace("hdfs://" + parts[2], "");
//        
//        Inode inode = inodes.getInodeAtPath(pathOfInode);
//        String inodeName = inode.getInodePK().getName();
//        
//        jobHistoryFac.persist(user, job, execId, exec.getAppId());
//        activityFacade.persistActivity(activityFacade.EXECUTED_JOB + inodeName, job.getProject(), user);
        break;
      case FLINK:
        return flinkController.startJob(job, user);
      case SPARK:
        exec = sparkController.startJob(job, user);
        if (exec == null) {
          throw new IllegalArgumentException("Problem getting execution object for: " + job.
              getJobType());
        }
        int execId = exec.getId();
        SparkJobConfiguration config = (SparkJobConfiguration) job.getJobConfig();

        String path = config.getJarPath();
        String patternString = "hdfs://(.*)\\s";
        Pattern p = Pattern.compile(patternString);
        Matcher m = p.matcher(path);
        String[] parts = path.split("/");
        String pathOfInode = path.replace("hdfs://" + parts[2], "");
        
        Inode inode = inodes.getInodeAtPath(pathOfInode);
        String inodeName = inode.getInodePK().getName();
        
        jobHistoryFac.persist(user, job, execId, exec.getAppId());
        activityFacade.persistActivity(activityFacade.EXECUTED_JOB + inodeName, job.getProject(), user);
        break;
      default:
        throw new IllegalArgumentException(
            "Unsupported job type: " + job.
            getJobType());
    }

    return exec;
  }

  public void stop(JobDescription job, Users user, String appid) throws
      IOException {
    switch (job.getJobType()) {
      case ADAM:
        adamController.stopJob(job, user, appid);
        break;
      case SPARK:
        sparkController.stopJob(job, user, appid);
        break;
      case FLINK:
        flinkController.stopJob(job, user, appid);
        break;
      default:
        throw new IllegalArgumentException("Unsupported job type: " + job.
            getJobType());
        
    }
  }
}
