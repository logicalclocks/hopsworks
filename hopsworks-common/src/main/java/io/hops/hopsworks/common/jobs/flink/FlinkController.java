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

import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.UserGroupInformationService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.configuration.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.security.UserNotFoundException;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.parquet.Strings;
import org.yaml.snakeyaml.Yaml;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interaction point between the Flink front- and backend.
 * <p>
 */
@Stateless
public class FlinkController {
  
  private static final Logger LOGGER = Logger.getLogger(FlinkController.class.getName());
  
  @EJB
  YarnJobsMonitor jobsMonitor;
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
  private YarnClientService ycs;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private InodeController inodeController;

  
  public Execution startJob(final Jobs job, final Users user) throws GenericException, JobException {
    //First: some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot run a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot run a job as a null user.");
    } else if (job.getJobType() != JobType.FLINK && job.getJobType() != JobType.BEAM_FLINK) {
      throw new IllegalArgumentException(
        "Job configuration is not a Flink or Beam job configuration.");
    }
    
    String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
    FlinkJob flinkjob = null;
    try {
      UserGroupInformation proxyUser = ugiService.getProxyUser(username);
      try {
        flinkjob = proxyUser.doAs((PrivilegedExceptionAction<FlinkJob>) () -> new FlinkJob(job, submitter, user,
          hdfsUsersBean.getHdfsUserName(job.getProject(), job.getCreator()), jobsMonitor, settings));
      } catch (InterruptedException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    } catch (IOException ex) {
      throw new JobException(RESTCodes.JobErrorCode.PROXY_ERROR, Level.SEVERE,
        "job: " + job.getId() + ", user:" + user.getUsername(), ex.getMessage(), ex);
    }
    if (flinkjob == null) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.WARNING,
        "Could not instantiate job with name: " + job.getName() + " and id: " + job.getId(),
        "sparkjob object was null");
    }
    Execution execution = flinkjob.requestExecutionId();
    submitter.startExecution(flinkjob);
    activityFacade.persistActivity(ActivityFacade.RAN_JOB, job.getProject(), user.asUser(), ActivityFlag.JOB);
    
    return execution;
  }
  
  /**
   * Retrieves the Flink master address from a running Flink session in YARN.
   *
   * @param appId flink ApplicationId in YARN
   * @return String of ip:port of flink master
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public String getFlinkMasterAddr(String appId) {
    LOGGER.log(Level.INFO, "Getting Flink Master Addr for:" + appId);
    Configuration conf = settings.getConfiguration();
    org.apache.flink.configuration.Configuration flinkConf
      = org.apache.flink.configuration.GlobalConfiguration.loadConfiguration(settings.getFlinkConfDir());
    YarnConfiguration yarnConf = new YarnConfiguration(conf);
    YarnClientWrapper yarnClientWrapper = null;
    YarnClusterDescriptor cluster = null;
    String flinkMasterURL = null;
    try {
      yarnConf.addResource(new File(settings.getHadoopConfDir() + "/yarn-site.xml").toURI().toURL());
      yarnClientWrapper = ycs.getYarnClientSuper();
      YarnClient yarnClient = yarnClientWrapper.getYarnClient();
      cluster = new YarnClusterDescriptor(flinkConf,
        yarnConf, settings.getFlinkConfDir(), yarnClient, true);
      ClusterClient<ApplicationId> clusterClient = cluster.retrieve(ApplicationId.fromString(appId));
      flinkMasterURL = clusterClient.getClusterConnectionInfo().getHostname() + ":" +
        clusterClient.getClusterConnectionInfo().getPort();
    } catch (Exception ex) {
      LOGGER.log(Level.FINE, "Could not retrieve Flink Master URL for applicationID: " + appId, ex);
    } finally {
      if (cluster != null) {
        cluster.close();
      }
      if (yarnClientWrapper != null) {
        ycs.closeYarnClient(yarnClientWrapper);
      }
    }
    return flinkMasterURL;
  }
  
  /**
   * Used by flinkCompletedJobsCache() on startUp.
   *
   * @param archiveDir Path where Flink conf is.
   *
   * @return Map of "<job,project>"
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Map<String, Project> getProjectsOfFlinkJobs(String archiveDir) {
    if (Strings.isNullOrEmpty(archiveDir)) {
      throw new IllegalArgumentException("Flink historyserver.archive.fs.dir property was not provided");
    }
    Map<String, Project> projectsJobs = new HashMap<>();
    //Read all completed jobs from "historyserver.archive.fs.dir"
    try {
      List<Inode> jobs = inodeController.getChildren(archiveDir);
      for (Inode job : jobs) {
        if (job.getHdfsUser() != null) {
          projectsJobs.put(job.getInodePK().getName(), projectFacade.findByName(job.getHdfsUser().getProject()));
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Could not find Flink jobs in history server archive " + archiveDir, e);
    }
    return projectsJobs;
  }
  
  
  /**
   * Reads all the completed jobs from the "historyserver.archive.fs.dir" in flink-conf.yaml and creates a map with
   * key the project and value a list of Flink jobs that ran in this project.
   *
   * @return a map with key the unique flink job id and value the project name.
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Project getProjectOfFlinkJob(String archivePath, String job) throws UserNotFoundException {
    if (Strings.isNullOrEmpty(archivePath)) {
      throw new IllegalArgumentException("Flink historyserver.archive.fs.dir property was not provided");
    }
    //Read all completed jobs from "historyserver.archive.fs.dir"
    //Flink history server caches old jobs locally. Server is setup to restart once a day, but until it does we might
    // can jobs that don't exist
    Inode inode = inodeController.getInodeAtPath(archivePath + File.separator + job);
    if (inode != null) {
      HdfsUsers hdfsUser = inode.getHdfsUser();
      if (hdfsUser != null) {
        return projectFacade.findByName(hdfsUser.getProject());
      }
    }
    throw new UserNotFoundException("Flink job belongs to a deleted project or a removed project member.");
  }
  
  /**
   * read historyserver.archive.fs.dir path from flink-conf.yaml
   * @return the value of historyserver.archive.fs.dir
   * @throws IOException If the flink conf file could be read.
   */
  public String getArchiveDir() throws IOException {
    //R
    Yaml yaml = new Yaml();
    try (InputStream in = new FileInputStream(new File(settings.getFlinkConfFile()))) {
      Map<String, Object> obj = (Map<String, Object>) yaml.load(in);
      return Utils.prepPath((String) obj.get("historyserver.archive.fs.dir"));
    }
  }
  
}
