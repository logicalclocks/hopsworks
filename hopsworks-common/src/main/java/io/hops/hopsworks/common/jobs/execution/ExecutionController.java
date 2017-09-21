package io.hops.hopsworks.common.jobs.execution;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.JobsHistoryFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.adam.AdamController;
import io.hops.hopsworks.common.jobs.flink.FlinkController;
import io.hops.hopsworks.common.jobs.spark.SparkController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jobs.tensorflow.TensorFlowController;
import io.hops.hopsworks.common.util.Settings;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes care of booting the execution of a job.
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
  private TensorFlowController tensorflowController;
  @EJB
  private InodeFacade inodes;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private JobsHistoryFacade jobHistoryFac;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private Settings settings;
  @EJB
  private ExecutionFacade execFacade;

  private final static Logger LOGGER = Logger.getLogger(ExecutionController.class.getName());

  public Execution start(Jobs job, Users user, String sessionId) throws IOException {
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
//        String path = adamConfig.getAppPath();
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
        return flinkController.startJob(job, user, sessionId);
      case SPARK:
        exec = sparkController.startJob(job, user, sessionId);
        if (exec == null) {
          throw new IllegalArgumentException(
                  "Problem getting execution object for: " + job.
                  getJobType());
        }
        int execId = exec.getId();
        SparkJobConfiguration config = (SparkJobConfiguration) job.
                getJobConfig();

        String path = config.getAppPath();
        String patternString = "hdfs://(.*)\\s";
        Pattern p = Pattern.compile(patternString);
        Matcher m = p.matcher(path);
        String[] parts = path.split("/");
        String pathOfInode = path.replace("hdfs://" + parts[2], "");

        Inode inode = inodes.getInodeAtPath(pathOfInode);
        String inodeName = inode.getInodePK().getName();

        jobHistoryFac.persist(user, job, execId, exec.getAppId());
        activityFacade.persistActivity(activityFacade.EXECUTED_JOB + inodeName,
                job.getProject(), user);
        break;
      case PYSPARK:
      case TFSPARK:
        exec = sparkController.startJob(job, user, sessionId);
        if (exec == null) {
          throw new IllegalArgumentException("Problem getting execution object for: " + job.getJobType());
        }
        break;
      case TENSORFLOW:
        return tensorflowController.startJob(job, user);
      default:
        throw new IllegalArgumentException(
                "Unsupported job type: " + job.
                getJobType());
    }

    return exec;
  }

  public void kill(Jobs job, Users user) throws IOException {
    //Get the lastest appId for the job, a job cannot have to concurrent application running.
    List<Execution> jobExecs = execFacade.findForJob(job);
    //Sort descending based on jobId
    Collections.sort(jobExecs, new Comparator<Execution>() {
      @Override
      public int compare(Execution lhs, Execution rhs) {
        return lhs.getId() > rhs.getId() ? -1 : (lhs.getId() < rhs.getId()) ? 1 : 0;
      }
    });
    String appId = jobExecs.get(0).getAppId();
    //Look for unique marker file which means it is a streaming job. Otherwise proceed with normal kill.
    DistributedFileSystemOps udfso = null;
    String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
    try {
      udfso = dfs.getDfsOps(username);
      String marker = Settings.getJobMarkerFile(job, appId);
      if (udfso.exists(marker)) {
        udfso.rm(new org.apache.hadoop.fs.Path(marker), false);
      } else {
        //WORKS FOR NOW BUT SHOULD EVENTUALLY GO THROUGH THE YARN CLIENT API
        Runtime rt = Runtime.getRuntime();
        rt.exec(settings.getHadoopSymbolicLinkDir() + "/bin/yarn application -kill " + appId);
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Could not remove marker file for job:" + job.getName() + "with appId:" + appId, ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void stop(Jobs job, Users user, String appid) throws
          IOException {
    switch (job.getJobType()) {
      case ADAM:
        adamController.stopJob(job, user, appid);
        break;
      case SPARK:
        sparkController.stopJob(job, user, appid);
        break;
      case FLINK:
        flinkController.stopJob(job, user, appid, null);
        break;
      default:
        throw new IllegalArgumentException("Unsupported job type: " + job.
                getJobType());

    }
  }
}
