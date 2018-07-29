/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.jobs.flink;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
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
  public Execution startJob(final Jobs job, final Users user, String sessionId) throws
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
              settings.getHadoopSymbolicLinkDir(), settings.getFlinkDir(),
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

  public void stopJob(Jobs job, Users user, String appid, String sessionId) throws
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
        settings.getHadoopSymbolicLinkDir(), settings.getFlinkDir(),
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
