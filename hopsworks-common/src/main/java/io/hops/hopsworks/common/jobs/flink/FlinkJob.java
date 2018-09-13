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
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.Path;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJob;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.yarn.client.api.YarnClient;

/**
 * Orchestrates the execution of a Flink job: run job, update history object.
 * <p>
 */
public class FlinkJob extends YarnJob {

  private static final Logger LOG = Logger.getLogger(
      FlinkJob.class.getName());
  private final FlinkJobConfiguration jobconfig;
  private final String flinkDir;
  private final String glassfishDomainDir;
  private final String flinkConfDir;
  private final String flinkConfFile;
  private final String flinkUser;
  private final String JOBTYPE_STREAMING = "Streaming";

  /**
   *
   * @param job
   * @param services
   * @param user
   * @param hadoopDir
   * @param flinkDir
   * @param flinkConfDir
   * @param flinkConfFile
   * @param flinkUser
   * @param jobUser
   * @param glassfishDomainsDir
   * @param jobsMonitor
   * @param settings
   * @param sessionId
   */
  public FlinkJob(Jobs job, AsynchronousJobExecutor services,
      Users user, final String hadoopDir,
      final String flinkDir, final String flinkConfDir,
      final String flinkConfFile, String flinkUser,
      String jobUser, final String glassfishDomainsDir, YarnJobsMonitor jobsMonitor,
      Settings settings, String sessionId) {
    super(job, services, user, jobUser, hadoopDir, jobsMonitor, settings, sessionId);
    if (!(job.getJobConfig() instanceof FlinkJobConfiguration)) {
      throw new IllegalArgumentException(
          "Job must contain a FlinkJobConfiguration object. Received: "
          + job.getJobConfig().getClass());
    }
    this.jobconfig = (FlinkJobConfiguration) job.getJobConfig();
    this.jobconfig.setFlinkConfDir(flinkConfDir);
    this.jobconfig.setFlinkConfFile(flinkConfFile);
    this.flinkDir = flinkDir;
    this.glassfishDomainDir = glassfishDomainsDir;
    this.flinkConfDir = flinkConfDir;
    this.flinkConfFile = flinkConfFile;
    this.flinkUser = flinkUser;
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso, YarnClient yarnClient) {
    super.setupJob(dfso, yarnClient);
    
    //Then: actually get to running.
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Flink Job");
    }

    FlinkYarnRunnerBuilder flinkBuilder = new FlinkYarnRunnerBuilder(
        jobconfig.getJarPath(), jobconfig.getMainClass());
    //https://ci.apache.org/projects/flink/flink-docs-release-0.10/setup/yarn_setup.html
    /*
     * If you do not want to keep the Flink YARN client running all the time,
     * its also possible to start a detached YARN session. The parameter for
     * that is called -d or --detached. In that case, the Flink YARN client
     * will only submit Flink to the cluster and then close itself.
     */
    flinkBuilder.setDetachedMode(true);
    flinkBuilder.setName(jobconfig.getAppName());
    flinkBuilder.setConfigurationDirectory(jobconfig.getFlinkConfDir());
    flinkBuilder.setConfigurationFilePath(new Path(
        jobconfig.getFlinkConfFile()));
    //Flink specific conf object
    flinkBuilder.setFlinkLoggingConfigurationPath(new Path(
        jobconfig.getFlinkConfDir()));

    flinkBuilder.setTaskManagerMemory(jobconfig.getTaskManagerMemory());
    flinkBuilder.setTaskManagerSlots(jobconfig.getSlots());
    flinkBuilder.setTaskManagerCount(jobconfig.getNumberOfTaskManagers());
    if (jobconfig.getFlinkjobtype().equals(JOBTYPE_STREAMING)) {
      flinkBuilder.setStreamingMode(true);
    }
    flinkBuilder.setParallelism(jobconfig.getParallelism());
    flinkBuilder.setJobManagerMemory(jobconfig.getAmMemory());
    flinkBuilder.setJobManagerQueue(jobconfig.getAmQueue());
    flinkBuilder.setAppJarPath(jobconfig.getJarPath());
    //Set Kafka params
    flinkBuilder.setServiceProps(serviceProps);
    flinkBuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
    //Set project specific resources, i.e. Kafka certificates
    flinkBuilder.addExtraFiles(projectLocalResources);
    if (jobconfig.getArgs() != null && !jobconfig.getArgs().isEmpty()) {
      String[] jobArgs = jobconfig.getArgs().trim().split(" ");
      flinkBuilder.addAllJobArgs(jobArgs);
    }
    if (jobSystemProperties != null && !jobSystemProperties.isEmpty()) {
      for (Map.Entry<String, String> jobSystemProperty : jobSystemProperties.
          entrySet()) {
        flinkBuilder.addSystemProperty(jobSystemProperty.getKey(), jobSystemProperty.getValue());
      }
    }
    try {
      runner = flinkBuilder.
          getYarnRunner(jobs.getProject().getName(),
              flinkUser, jobUser, hadoopDir, flinkDir, flinkConfDir,
              flinkConfFile, services.getFileOperations
                  (hdfsUser.getUserName()), yarnClient, glassfishDomainDir +
                  "/domain1/config/", services);

    } catch (IOException e) {
      LOG.log(Level.SEVERE,
          "Failed to create YarnRunner.", e);
      try {
        writeToLogs("Failed to start Yarn client.");
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Failed to write logs for failed application.", e);
      }
      return false;
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(
        jobs.
            getProject().
            getName())
        + Settings.FLINK_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getHdfsRootPath(
        jobs.
            getProject().
            getName())
        + Settings.FLINK_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);
    return true;
  }

  @Override
  protected void cleanup() {
    LOG.log(Level.INFO, "Job finished performing cleanup...");
    if (monitor != null) {
      monitor.close();
      monitor = null;
    }
    //Remove local files required for the job (Kafka certs etc.)
    //Search for other jobs using Kafka in the same project. If any active
    //ones are found

    Collection<ProjectServices> projectServices = jobs.getProject().
        getProjectServicesCollection();
    Iterator<ProjectServices> iter = projectServices.iterator();
    boolean removeKafkaCerts = true;
    while (iter.hasNext()) {
      ProjectServices projectService = iter.next();
      //If the project is of type KAFKA
      if (projectService.getProjectServicesPK().getService()
          == ProjectServiceEnum.KAFKA) {
        List<Execution> execs = services.getExecutionFacade().
            findForProjectByType(jobs.getProject(), JobType.FLINK);
        if (execs != null) {
          execs.addAll(services.getExecutionFacade().
              findForProjectByType(jobs.getProject(),
                  JobType.SPARK));
        }
        //Find if this project has running jobs
        if (execs != null && !execs.isEmpty()) {
          for (Execution exec : execs) {
            if (!exec.getState().isFinalState()) {
              removeKafkaCerts = false;
              break;
            }
          }
        }
      }
    }
    if (removeKafkaCerts) {
      String k_certName = jobs.getProject().getName() + "__"
          + jobs.getProject().getOwner().getUsername()
          + "__kstore.jks";
      String t_certName = jobs.getProject().getName() + "__"
          + jobs.getProject().getOwner().getUsername()
          + "__tstore.jks";
      File k_cert = new File(glassfishDomainDir + "/domain1/config/"
          + k_certName);
      File t_cert = new File(glassfishDomainDir + "/domain1/config/"
          + t_certName);
      if (k_cert.exists()) {
        k_cert.delete();
      }
      if (t_cert.exists()) {
        t_cert.delete();
      }
    }

  }

  @Override
  protected void stopJob(String appid) {
    //Stop flink cluster first
    try {
      Runtime rt = Runtime.getRuntime();
      Process pr = rt.exec(this.hadoopDir + "/bin/yarn application -kill "
          + appid);
    } catch (IOException ex1) {
      LOG.log(Level.SEVERE, "Unable to stop flink cluster with appID:"
          + appid, ex1);
    }
  }

}
