package io.hops.hopsworks.common.jobs.tensorflow;

import io.hops.hopsworks.common.dao.hdfs.HdfsLeDescriptorsFacade;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.UserGroupInformationService;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;

/**
 *
 * <p>
 */
@Stateless
public class TensorFlowController {

  private static final Logger LOGGER = Logger.getLogger(TensorFlowController.class.getName());

  @EJB
  private AsynchronousJobExecutor submitter;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private UserGroupInformationService ugiService;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private Settings settings;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;
  @EJB
  private YarnJobsMonitor jobsMonitor;

  public Execution startJob(final JobDescription job, final Users user) throws
      IllegalStateException,
      IOException, NullPointerException, IllegalArgumentException {
    //First: some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot run a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot run a job as a null user.");
    } else if (job.getJobType() != JobType.TENSORFLOW) {
      throw new IllegalArgumentException(
          "Job configuration is not a TensorFlow job configuration.");
    }

    String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
    UserGroupInformation proxyUser = ugiService.getProxyUser(username);
    TensorFlowJob tfJob = null;
    try {
      tfJob = proxyUser.doAs(new PrivilegedExceptionAction<TensorFlowJob>() {
        @Override
        public TensorFlowJob run() throws Exception {
          return new TensorFlowJob(job, submitter, user,
              settings.getHadoopDir(), hdfsLeDescriptorsFacade.getSingleEndpoint(),
              settings.getHdfsSuperUser(),
              hdfsUsersBean.getHdfsUserName(job.getProject(), job.getCreator()),
              jobsMonitor);
        }
      });
    } catch (InterruptedException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    if (tfJob == null) {
      throw new NullPointerException("Could not instantiate Flink job.");
    }
    Execution execution = tfJob.requestExecutionId();
    if (execution != null) {
      submitter.startExecution(tfJob);
    } else {
      LOGGER.log(Level.SEVERE,
          "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activityFacade.persistActivity(ActivityFacade.RAN_JOB, job.getProject(),
        user.asUser());
    return execution;
  }

  public void stopJob(JobDescription job, Users user, String appid) throws
      IllegalStateException,
      IOException, NullPointerException, IllegalArgumentException {
    //First: some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot stop a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot stop a job as a null user.");
    } else if (job.getJobType() != JobType.TENSORFLOW) {
      throw new IllegalArgumentException(
          "Job configuration is not a TensorFlow job configuration.");
    }

    TensorFlowJob tfJob = new TensorFlowJob(job, submitter, user,
        settings.getHadoopDir(), hdfsLeDescriptorsFacade.getSingleEndpoint(),
        settings.getHdfsSuperUser(),
        hdfsUsersBean.getHdfsUserName(job.getProject(), job.getCreator()),jobsMonitor);

    submitter.stopExecution(tfJob, appid);

  }

  /**
   * Inspect the jar or.py on the given path for execution. Returns a
   * SparkJobConfiguration object with a default
   * configuration for this job.
   * <p/>
   * @param path
   * @param username the user name in a project (projectName__username)
   * @param dfso
   * @return
   * @throws org.apache.hadoop.security.AccessControlException
   * @throws IOException
   */
  public TensorFlowJobConfiguration inspectProgram(String path, String username,
      DistributedFileSystemOps dfso) throws
      AccessControlException, IOException,
      IllegalArgumentException {
    LOGGER.log(Level.INFO, "Executing TensorFlow job by {0} at path: {1}",
        new Object[]{username, path});
    if (!path.endsWith(".py")) {
      throw new IllegalArgumentException("Path does not point to a .py file.");
    }
//    HdfsLeDescriptors hdfsLeDescriptors = hdfsLeDescriptorsFacade.findEndpoint();
//    // If the hdfs endpoint (ip:port - e.g., 10.0.2.15:8020) is missing, add it.
//    path = path.replaceFirst("hdfs:/*Projects",
//        "hdfs://" + hdfsLeDescriptors.getHostname() + "/Projects");
    LOGGER.log(Level.INFO, "Really executing TensorFlow job by {0} at path: {1}",
        new Object[]{username, path});
    TensorFlowJobConfiguration config = new TensorFlowJobConfiguration();

    config.setAppPath(path);
    return config;
  }

}
