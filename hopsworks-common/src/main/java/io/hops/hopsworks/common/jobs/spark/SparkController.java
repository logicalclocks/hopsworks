/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.jobs.spark;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
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
import io.hops.hopsworks.common.hdfs.UserGroupInformationService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;

/**
 * Interaction point between the Spark front- and backend.
 * <p/>
 */
@Stateless
public class SparkController {

  private static final Logger LOG = Logger.getLogger(SparkController.class.
      getName());
  @EJB
  private YarnJobsMonitor jobsMonitor;
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
   * Start the Spark job as the given user.
   * <p/>
   * @param job
   * @param user
   * @return
   * @throws IllegalStateException If Spark is not set up properly.
   * @throws IOException If starting the job fails.
   * @throws NullPointerException If job or user is null.
   * @throws IllegalArgumentException If the given job does not represent a
   * Spark job.
   */
  public Execution startJob(final Jobs job, final Users user) throws
      IllegalStateException,
      IOException, NullPointerException, IllegalArgumentException {
    //First: some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot run a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot run a job as a null user.");
    } else if (job.getJobType() != JobType.SPARK && job.getJobType() != JobType.PYSPARK && job.getJobType()
        != JobType.TFSPARK) {
      throw new IllegalArgumentException(
          "Job configuration is not a Spark job configuration.");
    }

    String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
    UserGroupInformation proxyUser = ugiService.getProxyUser(username);
    SparkJob sparkjob = null;
    try {
      sparkjob = proxyUser.doAs(new PrivilegedExceptionAction<SparkJob>() {
        @Override
        public SparkJob run() throws Exception {
          return new SparkJob(job, submitter, user, settings.getHadoopSymbolicLinkDir(),
              job.getProject().getName() + "__"
              + user.getUsername(), jobsMonitor, settings);
        }
      });
    } catch (InterruptedException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
    if (sparkjob == null) {
      throw new NullPointerException("Could not instantiate Sparkjob.");
    }
    Execution jh = sparkjob.requestExecutionId();
    if (jh != null) {
      submitter.startExecution(sparkjob);
    } else {
      LOG.log(Level.SEVERE,
          "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activityFacade.persistActivity(ActivityFacade.RAN_JOB + job.getName(), job.
        getProject(),
        user.asUser());
    return jh;
  }

  public void stopJob(Jobs job, Users user, String appid) throws
      IllegalStateException,
      IOException, NullPointerException, IllegalArgumentException {
    //First: some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot stop a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot stop a job as a null user.");
    } else if (job.getJobType() != JobType.SPARK) {
      throw new IllegalArgumentException(
          "Job configuration is not a Spark job configuration.");
    }

    SparkJob sparkjob = new SparkJob(job, submitter, user, settings.getHadoopSymbolicLinkDir(),
        hdfsUsersBean.getHdfsUserName(job.getProject(), job.getCreator()), jobsMonitor, settings);
    submitter.stopExecution(sparkjob, appid);

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
  public SparkJobConfiguration inspectProgram(String path, String username,
      DistributedFileSystemOps dfso) throws
      AccessControlException, IOException,
      IllegalArgumentException {
    LOG.log(Level.INFO, "Executing Spark job by {0} at path: {1}",
        new Object[]{username, path});
    if (!path.endsWith(".jar") && !path.endsWith(".py")) {
      throw new IllegalArgumentException("Path does not point to a jar or .py file.");
    }
    LOG.log(Level.INFO, "Really executing Spark job by {0} at path: {1}",
        new Object[]{username, path});
    SparkJobConfiguration config = new SparkJobConfiguration();
    //If the main program is in a jar, try to set main class from it
    if (path.endsWith(".jar")) {
      JarInputStream jis = new JarInputStream(dfso.open(path));
      Manifest mf = jis.getManifest();
      if (mf != null) {
        Attributes atts = mf.getMainAttributes();
        if (atts.containsKey(Name.MAIN_CLASS)) {
          config.setMainClass(atts.getValue(Name.MAIN_CLASS));
        }
      }
    } else {
      config.setMainClass(Settings.SPARK_PY_MAINCLASS);
    }
    config.setAppPath(path);
    config.setHistoryServerIp(settings.getSparkHistoryServerIp());
    return config;
  }

}
