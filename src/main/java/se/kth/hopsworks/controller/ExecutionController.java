package se.kth.hopsworks.controller;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.ExecutionInputfilesFacade;
import se.kth.bbc.jobs.jobhistory.ExecutionsInputfiles;
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
  private CuneiformController cuneiformController;
  @EJB
  private SparkController sparkController;
  @EJB
  private AdamController adamController;
  @EJB
  private FlinkController flinkController;
  @EJB
  private InodeFacade inodes;
  @EJB
  private ExecutionInputfilesFacade execInputFilesFacade;
  
  final Logger logger = LoggerFactory.getLogger(ExecutionController.class);
  
  public Execution start(JobDescription job, Users user) throws IOException {
    Execution exec = null;

    switch (job.getJobType()) {
      //      case CUNEIFORM:
      //        return cuneiformController.startWorkflow(job, user);
      case ADAM:
        exec = adamController.startJob(job, user);
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
        logger.debug("The execution Id is: " + execId);
        SparkJobConfiguration config = (SparkJobConfiguration) job.getJobConfig();
        String args = config.getArgs();
        logger.debug("The args are: " + args);
        String path = config.getJarPath();
        logger.debug("The path is: " + path);
        String patternString = "hdfs://(.*)\\s";
        Pattern p = Pattern.compile(patternString);
        Matcher m = p.matcher(path);
        //execInputFilesFacade.create(execId, 8, "myFirstProject");
        int mCount = m.groupCount();
        if(m.groupCount()>0){
//        for (int i = 0; i < m.groupCount(); i++) { // for each filename, resolve Inode from HDFS filename
//          int mC;
//          //String filename = m.group();
//          mC = m.groupCount();
//          Inode inode = inodes.getInodeAtPath(path);
//          mC = m.groupCount();
//          int parentID = inode.getInodePK().getParentId();
//          mC = m.groupCount();
//          String name = inode.getInodePK().getName(); 
//          mC = m.groupCount();
//          execInputFilesFacade.create(execId, inode.getInodePK().getParentId(), inode.getInodePK().getName());
//          // insert into inputfiles_executions (inode, execId).
//       }
        for (int i = 0; i < m.groupCount(); i++) { // for each filename, resolve Inode from HDFS filename
//          String filename = m.group(i);
//           Inode inode = inodes.getInodeAtPath("hdfs://" + filename);
          // insert into inputfiles_executions (inode, execId).
        }
        }
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
      case CUNEIFORM:
      //cuneiformController.stopWorkflow(job, user);
      case ADAM:
      //adamController.stopJob(job, user);
      case SPARK:
      //sparkController.stopJob(job, user, appid);
      default:
        throw new IllegalArgumentException("Unsupported job type: " + job.
            getJobType());
    }
  }
}
