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

package io.hops.hopsworks.common.jobs.spark;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.UserGroupInformationService;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.security.UserGroupInformation;
import org.eclipse.persistence.exceptions.DatabaseException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interaction point between the Spark front- and backend.
 * <p/>
 */
@Stateless
public class SparkController {

  private static final Logger LOGGER = Logger.getLogger(SparkController.class.getName());
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
  @EJB
  private DistributedFsService dfs;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ExecutionController executionController;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private JobFacade jobFacade;

  /**
   * Start the Spark job as the given user.
   * <p/>
   * @param job
   * @param user
   * @return
   * @throws IllegalStateException If Spark is not set up properly.
   * @throws IOException If starting the job fails.
   * Spark job.
   */
  public Execution startJob(final Jobs job, final Users user) throws GenericException, JobException, ServiceException {
    //First: some parameter checking.
    sanityCheck(job, user);
    String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);

    SparkJobConfiguration sparkConfig = (SparkJobConfiguration)job.getJobConfig();
    String appPath = sparkConfig.getAppPath();

    //If it is a notebook we need to convert it to a .py file every time the job is run
    if(appPath.endsWith(".ipynb")) {
      int extensionIndex = appPath.lastIndexOf(".ipynb");
      StringBuilder pathBuilder = new StringBuilder(appPath.substring(0, extensionIndex)).append("_job_" +
              job.getName() + ".py");
      String pyAppPath = pathBuilder.toString();
      sparkConfig.setAppPath(pyAppPath);
      jupyterController.convertIPythonNotebook(username, appPath, job.getProject(), pyAppPath);
    }

    SparkJob sparkjob = null;
    try {
      UserGroupInformation proxyUser = ugiService.getProxyUser(username);
      try {
        sparkjob = proxyUser.doAs(new PrivilegedExceptionAction<SparkJob>() {
          @Override
          public SparkJob run() {
            return new SparkJob(job, submitter, user, settings.getHadoopSymbolicLinkDir(),
              job.getProject().getName() + "__"
                + user.getUsername(), jobsMonitor, settings);
          }
        });
      } catch (InterruptedException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    
    } catch (IOException ex) {
      throw new JobException(RESTCodes.JobErrorCode.PROXY_ERROR, Level.SEVERE,
        "job: " + job.getId() + ", user:" + user.getUsername(), ex.getMessage(), ex);
    }
    if (sparkjob == null) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.WARNING,
        "Could not instantiate job with name: " + job.getName() + " and id: " + job.getId(),
        "sparkjob object was null");
    }

    Execution jh = sparkjob.requestExecutionId();
    submitter.startExecution(sparkjob);
    activityFacade.persistActivity(ActivityFacade.RAN_JOB + job.getName(), job.getProject(), user.asUser(),
        ActivityFacade.ActivityFlag.JOB);
    return jh;
  }

  public void stopJob(Jobs job, Users user, String appid) {
    //First: some parameter checking.
    sanityCheck(job, user);
    SparkJob sparkjob = new SparkJob(job, submitter, user, settings.getHadoopSymbolicLinkDir(),
        hdfsUsersBean.getHdfsUserName(job.getProject(), job.getCreator()), jobsMonitor, settings);
    submitter.stopExecution(sparkjob, appid);
  }
  
  public void deleteJob(Jobs job, Users user) throws JobException {
    //Kill running execution of this job (if any)
    executionController.kill(job, user);
    try {
      LOGGER.log(Level.INFO, "Request to delete job name ={0} job id ={1}",
        new Object[]{job.getName(), job.getId()});
      jobFacade.removeJob(job);
      LOGGER.log(Level.INFO, "Deleted job name ={0} job id ={1}", new Object[]{job.getName(), job.getId()});
      activityFacade.persistActivity(ActivityFacade.DELETED_JOB + job.getName(), job.getProject(), user.getEmail(), 
          ActivityFacade.ActivityFlag.JOB);
    } catch (DatabaseException ex) {
      LOGGER.log(Level.SEVERE, "Job cannot be deleted job name ={0} job id ={1}",
        new Object[]{job.getName(), job.getId()});
      throw new JobException(RESTCodes.JobErrorCode.JOB_DELETION_ERROR, Level.SEVERE, ex.getMessage(), null, ex);
    }
  }
  
  private void sanityCheck(Jobs job, Users user) {
    if (job == null) {
      throw new IllegalArgumentException("Trying to start job but job is not provided");
    } else if (user == null) {
      throw new IllegalArgumentException("Trying to start job but user is not provided");
    } else if (job.getJobType() != JobType.SPARK && job.getJobType() != JobType.PYSPARK) {
      throw new IllegalArgumentException(
        "Job configuration is not a Spark job configuration. Type: " + job.getJobType());
    }
    SparkJobConfiguration jobConf = (SparkJobConfiguration) job.getJobConfig();
    if(jobConf == null) {
      throw new IllegalArgumentException("Trying to start job but JobConfiguration is null");
    }

    String path = jobConf.getAppPath();
    if (Strings.isNullOrEmpty(path) || !(path.endsWith(".jar") || path.endsWith(".py")
            || path.endsWith(".ipynb"))) {
      throw new IllegalArgumentException("Path does not point to a .jar, .py or .ipynb file.");
    }
  }
  
  public SparkJobConfiguration inspectProgram(String path, DistributedFileSystemOps udfso) throws JobException {
    SparkJobConfiguration config = new SparkJobConfiguration();
    //If the main program is in a jar, try to set main class from it
    if (path.endsWith(".jar")) {
      try (JarInputStream jis = new JarInputStream(udfso.open(path))) {
        Manifest mf = jis.getManifest();
        if (mf != null) {
          Attributes atts = mf.getMainAttributes();
          if (atts.containsKey(Attributes.Name.MAIN_CLASS)) {
            config.setMainClass(atts.getValue(Attributes.Name.MAIN_CLASS));
          }
        }
      } catch (IOException ex) {
        throw new JobException(RESTCodes.JobErrorCode.JAR_INSPECTION_ERROR, Level.SEVERE,
          "Failed to inspect jar at:" + path, ex.getMessage(), ex);
      }
    } else {
      config.setMainClass(Settings.SPARK_PY_MAINCLASS);
    }
    config.setAppPath(path);
    return config;
  }

}
