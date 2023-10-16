/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.jobs.scheduler;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobScheduleV2Facade;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.logging.Level;

import static com.cronutils.model.CronType.QUARTZ;


@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobScheduleV2Controller {

  @EJB
  private JobScheduleV2Facade jobScheduleFacade;
  @Inject
  private ExecutionController executionController;
  @Inject
  private ExecutionFacade executionFacade;

  private void executeSingle(Jobs job, Instant currentTime)
          throws JobException, ProjectException, ServiceException, GenericException {
    String jobConfig = job.getJobConfig().getDefaultArgs();
    if (Strings.isNullOrEmpty(jobConfig)) {
      jobConfig = "-start_time " + currentTime.toString();
    } else {
      jobConfig = jobConfig + " -start_time " + currentTime.toString();
    }
    executionController.start(job, jobConfig, job.getCreator());
  }
  
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void executeWithCron() throws JobException, ProjectException, ServiceException, GenericException {
    executeWithCron(Instant.now());
  }
  
  /**
   * For test purpose, so that job start time can be verified in unit test.
   *
   * @param currentTime
   * @throws JobException
   * @throws ProjectException
   * @throws ServiceException
   * @throws GenericException
   */
  public void executeWithCron(Instant currentTime)
          throws JobException, ProjectException, ServiceException, GenericException {
    for (JobScheduleV2 jobSchedule : jobScheduleFacade.getActiveWithCurrentExecutionTime(currentTime)) {
      if (executionFacade.findByJobAndNotFinished(jobSchedule.getJob()).size() > 0) {
        // Should not do parallel executions to void issues with Hudi
        continue;
      }
      updateNextExecutionDateTime(currentTime, jobSchedule);
      executeSingle(jobSchedule.getJob(), jobSchedule.getNextExecutionDateTime());
    }
  }

  public JobScheduleV2 createSchedule(JobScheduleV2 jobSchedule) {
    jobSchedule.setStartDateTime(jobSchedule.getStartDateTime());
    setNextExecutionDateTime(Instant.now(), jobSchedule);
    return jobScheduleFacade.update(jobSchedule);
  }

  public void deleteSchedule(Integer jobId) {
    jobScheduleFacade.removeByJobId(jobId);
  }

  public JobScheduleV2 updateSchedule(JobScheduleV2DTO jobScheduleV2DTO) throws JobException {
    JobScheduleV2 jobSchedule = jobScheduleFacade.getById(jobScheduleV2DTO.getId())
            .orElseThrow(() -> new JobException(RESTCodes.JobErrorCode.JOB_SCHEDULE_NOT_FOUND, Level.FINE));
    jobSchedule.setEnabled(jobScheduleV2DTO.getEnabled());
    jobSchedule.setCronExpression(jobScheduleV2DTO.getCronExpression());
    jobSchedule.setStartDateTime(jobScheduleV2DTO.getStartDateTime());
    jobSchedule.setEndDateTime(jobScheduleV2DTO.getEndDateTime());

    if (jobSchedule.getStartDateTime() != null &&
            jobSchedule.getNextExecutionDateTime() != null &&
            jobSchedule.getNextExecutionDateTime().isBefore(jobSchedule.getStartDateTime())) {
      // if the new start time is after the existing scheduled execution, set the existing scheduled execution to null
      jobSchedule.setNextExecutionDateTime(null);
    }

    if (jobSchedule.getEndDateTime() != null &&
            jobSchedule.getNextExecutionDateTime() != null &&
            jobSchedule.getNextExecutionDateTime().isAfter(jobSchedule.getEndDateTime())) {
      // if the next execution time is after the new end date, then disable the scheduling
      jobSchedule.setEnabled(false);
      jobSchedule.setNextExecutionDateTime(null);
    }

    if (jobSchedule.getEnabled()) {
      // Recompute start time
      setNextExecutionDateTime(Instant.now(), jobSchedule);
    }

    return jobScheduleFacade.update(jobSchedule);
  }

  public JobScheduleV2 getScheduleByJobId(Integer jobId) throws JobException {
    return jobScheduleFacade.getByJobId(jobId)
            .orElseThrow(() -> new JobException(RESTCodes.JobErrorCode.JOB_SCHEDULE_NOT_FOUND,
                    Level.FINE, String.format("There is no schedule for this job id `%d`.", jobId)));
  }

  public Instant computeNextExecutionDateTime(Instant currentTime, JobScheduleV2 jobSchedule) {
    return computeNextExecutionDateTime(currentTime,
            jobSchedule.getNextExecutionDateTime(),
            jobSchedule.getStartDateTime(),
            jobSchedule.getEndDateTime(),
            jobSchedule.getCronExpression());
  }

  public Instant computeNextExecutionDateTime(Instant currentTime,
                                              Instant currentExecutionTime,
                                              Instant startTime, Instant endTime, String cronExpression) {
    Instant computeFrom = null;
    if (currentExecutionTime != null) {
      // if there was already a scheduled execution, pick up from the last execution
      computeFrom = currentExecutionTime.plusMillis(1);
    } else if (startTime != null) {
      // if there is no previous scheduled execution, use the startTime (if provided by the user)
      computeFrom = startTime;
    } else {
      // if startTime was not provided, computed from the current time
      computeFrom = currentTime;
    }

    CronParser quartzCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
    Cron parsedCronExpression = quartzCronParser.parse(cronExpression);
    ExecutionTime executionTime = ExecutionTime.forCron(parsedCronExpression);
    Optional<ZonedDateTime> nextExecutionTime = executionTime.nextExecution(
      computeFrom.atZone(ZoneId.of("UTC"))
    );
    if (!nextExecutionTime.isPresent()) {return null;}

    if (endTime == null || nextExecutionTime.get().toInstant().isBefore(endTime)) {
      // the next execution time is before the end time configured by the user
      return nextExecutionTime.get().toInstant();
    } else {
      // the next execution time is after the end time configured by the user so return null
      // to disable the scheduling
      return null;
    }
  }
  
  private void setNextExecutionDateTime(Instant currentTime, JobScheduleV2 jobSchedule) {
    Instant nextExecutionTime = computeNextExecutionDateTime(currentTime, jobSchedule);
    if (nextExecutionTime == null) {
      jobSchedule.setEnabled(false);
    }
    jobSchedule.setNextExecutionDateTime(nextExecutionTime);
  }
  
  private JobScheduleV2 updateNextExecutionDateTime(Instant currentTime, JobScheduleV2 jobSchedule) {
    setNextExecutionDateTime(currentTime, jobSchedule);
    return jobScheduleFacade.update(jobSchedule);
  }
}
