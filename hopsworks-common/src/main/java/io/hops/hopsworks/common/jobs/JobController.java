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

package io.hops.hopsworks.common.jobs;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.bind.JAXBException;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.JobException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.configuration.ScheduleDTO;
import io.hops.hopsworks.common.jobs.flink.FlinkController;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkController;

@Stateless
public class JobController {
  
  @EJB
  private JobFacade jobFacade;
  @EJB
  private JobScheduler scheduler;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private SparkController sparkController;
  @EJB
  private FlinkController flinkController;
  
  private static final Logger LOGGER = Logger.getLogger(JobController.class.getName());
  
  public Jobs putJob(Users user, Project project, Jobs job, JobConfiguration config) throws JobException {
    try {
      job = jobFacade.put(user, project, config, job);
    } catch (IllegalStateException ise) {
      if (ise.getCause() instanceof JAXBException) {
        throw new JobException(RESTCodes.JobErrorCode.JOB_CONFIGURATION_CONVERT_TO_JSON_ERROR, Level.FINE,
          "Unable to create json from JobConfiguration", ise.getMessage(), ise);
      } else {
        throw ise;
      }
    }
    
    if (config.getSchedule() != null) {
      scheduler.scheduleJobPeriodic(job);
    }
  
    activityFacade.persistActivity(ActivityFacade.CREATED_JOB + getJobNameForActivity(job.getName()), project, user,
      ActivityFacade.ActivityFlag.JOB);
    return job;
  }
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void updateSchedule(Project project, Jobs job, ScheduleDTO schedule, Users user) throws JobException {
    boolean isScheduleUpdated = jobFacade.updateJobSchedule(job.getId(), schedule);
    if (isScheduleUpdated) {
      job.getJobConfig().setSchedule(schedule);
      scheduler.scheduleJobPeriodic(job);
      activityFacade.persistActivity(ActivityFacade.SCHEDULED_JOB + getJobNameForActivity(job.getName()), project, user,
        ActivityFacade.ActivityFlag.JOB);
    } else {
      throw new JobException(RESTCodes.JobErrorCode.JOB_SCHEDULE_UPDATE, Level.WARNING,
        "Schedule is not updated in the database for jobid: " + job.getId());
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public boolean unscheduleJob(Jobs job) {
    if (job.getJobConfig().getSchedule() != null) {
      boolean status = scheduler.unscheduleJob(job);
      job.getJobConfig().setSchedule(null);
      jobFacade.updateJobSchedule(job.getId(), null);
      if (!status) {
        LOGGER.log(Level.WARNING, "Schedule does not exist in the scheduler for jobid {0}", job.getId());
      }
    }
    return scheduler.unscheduleJob(job);
  }
  
  
  public Jobs getJob(Project project, String name) throws JobException {
    if(Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("job name was not provided or it was not set.");
    }
    Jobs job = jobFacade.findByProjectAndName(project, name);
    if (job == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_NOT_FOUND, Level.FINEST, "jobId:" + name);
    }
    return job;
  }
  
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public JobConfiguration inspectProgram(String path, Project project, Users user, JobType jobType)
          throws JobException {
    DistributedFileSystemOps udfso = null;
    try {
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      LOGGER.log(Level.INFO, "Inspecting executable job program by {0} at path: {1}", new Object[]{username, path});
      if (Strings.isNullOrEmpty(path) || !(path.endsWith(".jar") || path.endsWith(".py")
              || path.endsWith(".ipynb"))) {
        throw new IllegalArgumentException("Path does not point to a .jar, .py or .ipynb file.");
      }
      switch (jobType){
        case SPARK:
        case PYSPARK:
          return sparkController.inspectProgram(path, udfso);
        case FLINK:
          return flinkController.inspectProgram(path, udfso);
        default:
          throw new IllegalArgumentException("Job type not supported: " + jobType);
      }
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private String getJobNameForActivity(String jobName) {
    String activityJobMsg = jobName;
    if (jobName.length() > 60) {
      activityJobMsg = jobName.substring(0, 60) + "...";
    }
    return activityJobMsg;
  }
}
