package io.hops.hopsworks.common.jobs.flink;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.UserGroupInformationService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;

/**
 * Interaction point between the Flink front- and backend.
 * <p>
 */
@Stateless
public class FlinkController {

  private static final Logger LOG = Logger.getLogger(FlinkController.class.
      getName());

  @EJB
  YarnJobsMonitor jobsMonitor;
  @EJB
  private DistributedFsService fops;
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

  /**
   * Start the Flink job as the given user.
   * <p/>
   * @param job
   * @param user
   * @param sessionId
   * @return
   * @throws IllegalStateException If Flink is not set up properly.
   * @throws IOException If starting the job fails.
   * @throws NullPointerException If job or user is null.
   * @throws IllegalArgumentException If the given job does not represent a
   * Flink job.
   */
  public Execution startJob(final JobDescription job, final Users user, String sessionId) throws
      IllegalStateException,
      IOException, NullPointerException, IllegalArgumentException {
    //First: some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot run a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot run a job as a null user.");
    } else if (job.getJobType() != JobType.FLINK) {
      throw new IllegalArgumentException(
          "Job configuration is not a Flink job configuration.");
    } else if (!isFlinkJarAvailable()) {
      throw new IllegalStateException("Flink is not installed on this system.");
    }

    String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
    UserGroupInformation proxyUser = ugiService.getProxyUser(username);
    FlinkJob flinkjob = null;
    try {
      flinkjob = proxyUser.doAs(new PrivilegedExceptionAction<FlinkJob>() {
        @Override
        public FlinkJob run() throws Exception {
          return new FlinkJob(job, submitter, user,
              settings.getHadoopDir(), settings.getFlinkDir(),
              settings.getFlinkConfDir(),
              settings.getFlinkConfFile(),
              settings.getFlinkUser(),
              hdfsUsersBean.getHdfsUserName(job.getProject(),
                  job.getCreator()),
              settings.getHopsworksDomainDir(), jobsMonitor, settings, sessionId);
        }
      });
    } catch (InterruptedException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    if (flinkjob == null) {
      throw new NullPointerException("Could not instantiate Flink job.");
    }
    Execution execution = flinkjob.requestExecutionId();
    if (execution != null) {
      submitter.startExecution(flinkjob);
    } else {
      LOG.log(Level.SEVERE,
          "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activityFacade.persistActivity(ActivityFacade.RAN_JOB, job.getProject(),
        user.asUser());
    return execution;
  }

  public void stopJob(JobDescription job, Users user, String appid, String sessionId) throws
      IllegalStateException,
      IOException, NullPointerException, IllegalArgumentException {
    //First: some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot stop a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot stop a job as a null user.");
    } else if (job.getJobType() != JobType.FLINK) {
      throw new IllegalArgumentException(
          "Job configuration is not a Flink job configuration.");
    } else if (!isFlinkJarAvailable()) {
      throw new IllegalStateException("Flink is not installed on this system.");
    }

    FlinkJob flinkJob = new FlinkJob(job, submitter, user,
        settings.getHadoopDir(), settings.getFlinkDir(),
        settings.getFlinkConfDir(), settings.getFlinkConfFile(),
        settings.getFlinkUser(),
        job.getProject().getName() + "__" + user.getUsername(),
        settings.getHopsworksDomainDir(), jobsMonitor, settings, sessionId);

    submitter.stopExecution(flinkJob, appid);

  }

  /**
   * Check if the Flink jar is in HDFS. If it's not, try and copy it there
   * from the local filesystem. If it's still not there, then return false.
   * <p/>
   * @return
   */
  public boolean isFlinkJarAvailable() {
    boolean isInHdfs;
    DistributedFileSystemOps dfso = null;
    try {
      dfso = fops.getDfsOps();
      try {
        isInHdfs = dfso.exists(settings.getHdfsFlinkJarPath());
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Cannot get Flink jar file from HDFS: {0}",
            settings.getHdfsFlinkJarPath());
        //Can't connect to HDFS: return false
        return false;
      }
      if (isInHdfs) {
        return true;
      }

      File localFlinkJar = new File(settings.getLocalFlinkJarPath());
      if (localFlinkJar.exists()) {
        try {
          String hdfsJarPath = settings.getHdfsFlinkJarPath();
          dfso.copyToHDFSFromLocal(false, settings.getLocalFlinkJarPath(),
              hdfsJarPath);
        } catch (IOException e) {
          return false;
        }
      } else {
        LOG.log(Level.WARNING, "Cannot find Flink jar file locally: {0}",
            settings.getLocalFlinkJarPath());
        return false;
      }
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
    return true;
  }

  /**
   * Inspect the jar on the given path for execution. Returns a
   * FlinkJobConfiguration object with a default
   * configuration for this job.
   * <p/>
   * @param path
   * @param username the user name in a project (projectName__username)
   * @param udfso
   * @return
   * @throws org.apache.hadoop.security.AccessControlException
   * @throws IOException
   */
  public FlinkJobConfiguration inspectJar(String path, String username,
      DistributedFileSystemOps udfso) throws
      AccessControlException, IOException,
      IllegalArgumentException {
    LOG.log(Level.INFO, "Executing Flink job by {0} at path: {1}", new Object[]{
      username, path});
    if (!path.endsWith(".jar")) {
      throw new IllegalArgumentException("Path does not point to a jar file.");
    }
    LOG.log(Level.INFO, "Really executing Flink job by {0} at path: {1}",
        new Object[]{username, path});

    JarInputStream jis = new JarInputStream(udfso.open(path));
    Manifest mf = jis.getManifest();
    Attributes atts = mf.getMainAttributes();
    FlinkJobConfiguration config = new FlinkJobConfiguration();
    if (atts.containsKey(Attributes.Name.MAIN_CLASS)) {
      config.setMainClass(atts.getValue(Attributes.Name.MAIN_CLASS));
    }
    //Set Flink config params
    config.setFlinkConfDir(settings.getFlinkConfDir());
    config.setFlinkConfFile(settings.getFlinkConfFile());

    config.setJarPath(path);
    return config;
  }
}
