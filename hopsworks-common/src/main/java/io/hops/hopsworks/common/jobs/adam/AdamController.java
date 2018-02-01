/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.jobs.adam;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.security.UserGroupInformation;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.hdfs.UserGroupInformationService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;

/**
 * Acts as the interaction point between the Adam frontend and backend.
 *
 */
@Stateless
public class AdamController {

  private static final Logger logger = Logger.getLogger(AdamController.class.
          getName());

  @EJB
  private YarnJobsMonitor jobsMonitor;
  @EJB
  private AsynchronousJobExecutor submitter;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private UserGroupInformationService ugiService;

  /**
   * Start an execution of the given job, ordered by the given User.
   *
   * @param job
   * @param user
   * @return
   * @throws IllegalStateException If Adam is not set up properly.
   * @throws IllegalArgumentException If the Jobs is not set up
 properly.
   * @throws IOException If starting the job fails.
   * @throws NullPointerException If job or user is null.
   */
  public Execution startJob(final Jobs job, final Users user) throws
          IllegalStateException,
          IllegalArgumentException, IOException, NullPointerException {
    //First: do some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot run a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot run a job as a null user.");
    } else if (job.getJobType() != JobType.ADAM) {
      throw new IllegalArgumentException(
              "The given job does not represent an Adam job.");
    }
    ((AdamJobConfiguration) job.getJobConfig()).setAppPath(settings.
            getAdamJarHdfsPath());
    ((AdamJobConfiguration) job.getJobConfig()).setHistoryServerIp(settings.
            getSparkHistoryServerIp());
    //Get to starting the job
    AdamJob adamJob = null;
    String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
    UserGroupInformation proxyUser = ugiService.getProxyUser(username);

    try {
      adamJob = proxyUser.doAs(new PrivilegedExceptionAction<AdamJob>() {
        @Override
        public AdamJob run() throws Exception {
          return new AdamJob(job, submitter, user, settings.getHadoopSymbolicLinkDir(),
                  settings.getSparkDir(), settings.getAdamUser(),
                  hdfsUsersBean.getHdfsUserName(job.getProject(), job.
                          getCreator()),
                  settings.getAdamJarHdfsPath(), jobsMonitor, settings);
        }
      });
    } catch (InterruptedException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    if (adamJob == null) {
      throw new NullPointerException("Could not instantiate Sparkjob.");
    }
    Execution jh = adamJob.requestExecutionId();
    if (jh != null) {
      submitter.startExecution(adamJob);
    } else {
      logger.log(Level.SEVERE,
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
    } else if (job.getJobType() != JobType.ADAM) {
      throw new IllegalArgumentException(
              "Job configuration is not a Spark job configuration.");
    }

    AdamJob adamJob = new AdamJob(job, submitter, user, settings.getHadoopSymbolicLinkDir(),
            settings.
            getSparkDir(), settings.getAdamUser(),
            hdfsUsersBean.getHdfsUserName(job.getProject(), job.
                    getCreator()),
            settings.getAdamJarHdfsPath(), jobsMonitor, settings);

    submitter.stopExecution(adamJob, appid);

  }

}
