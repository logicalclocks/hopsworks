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

package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobScheduleV2Facade;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2DTO;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2Controller;

import org.apache.avro.generic.GenericData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class TestJobSchedulerV2Controller {
  @InjectMocks
  private JobScheduleV2Controller target = new JobScheduleV2Controller();

  @Mock
  private JobScheduleV2Facade jobScheduleV2Facade;

  @Mock
  private ExecutionFacade executionFacade;

  // This is used within the tests and it needs to be mocked
  // otherwise a NullPointerException will be thrown.
  @Mock
  private ExecutionController executionController;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testComputeNextExecutionTimeStartDateTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
    calendar.set(2021, 1, 1, 0, 0, 0);

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setCronExpression("0 */10 * ? * * *");
    scheduler.setStartDateTime(calendar.getTime().toInstant());

    Instant nextExecutionDate = target.computeNextExecutionDateTime(Instant.now(), scheduler);

    calendar.setTime(Date.from(nextExecutionDate));
    Assert.assertEquals(0, calendar.get(Calendar.SECOND));
    Assert.assertEquals(10, calendar.get(Calendar.MINUTE));
  }

  @Test
  public void testComputeNextExecutionTimeNoStartDateTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
    calendar.set(2021, 1, 1, 0, 0, 0);

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setCronExpression("0 */10 * ? * * *");

    Instant nextExecutionDate = target.computeNextExecutionDateTime(calendar.toInstant(), scheduler);

    calendar.setTime(Date.from(nextExecutionDate));
    Assert.assertEquals(0, calendar.get(Calendar.SECOND));
    Assert.assertEquals(10, calendar.get(Calendar.MINUTE));
  }

  @Test
  public void testComputeNextExecutionTimePreviousExecution() {
    Calendar startDateTime = Calendar.getInstance();
    startDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    startDateTime.set(2021, 1, 1, 0, 0, 0);

    Calendar previousExecutionDateTime = Calendar.getInstance();
    previousExecutionDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    previousExecutionDateTime.set(2021, 1, 1, 0, 10, 0);

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setStartDateTime(startDateTime.toInstant());
    scheduler.setNextExecutionDateTime(previousExecutionDateTime.toInstant());
    scheduler.setCronExpression("0 */10 * ? * * *");

    Instant nextExecutionDate = target.computeNextExecutionDateTime(Instant.now(), scheduler);

    Calendar nextExecutionDateCalendar = Calendar.getInstance();
    nextExecutionDateCalendar.setTime(Date.from(nextExecutionDate));
    Assert.assertEquals(0, nextExecutionDateCalendar.get(Calendar.SECOND));
    Assert.assertEquals(20, nextExecutionDateCalendar.get(Calendar.MINUTE));
  }

  @Test
  public void testComputeNextExecutionTimeAfterEnd() {
    Calendar startDateTime = Calendar.getInstance();
    startDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    startDateTime.set(2021, 1, 1, 0, 0, 0);

    Calendar endExecutionTime = Calendar.getInstance();
    endExecutionTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    endExecutionTime.set(2021, 1, 1, 0, 5, 0);

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setStartDateTime(startDateTime.toInstant());
    scheduler.setEndDateTime(endExecutionTime.toInstant());
    scheduler.setCronExpression("0 */10 * ? * * *");

    Instant nextExecutionDate = target.computeNextExecutionDateTime(Instant.now(), scheduler);

    Assert.assertNull(nextExecutionDate);
  }

  // Check that on execution the nextExecutionDateTime is updated 
  @Test
  public void testExecuteWithCron() throws JobException, ProjectException, ServiceException, GenericException {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
    Instant currentTime = Instant.now();

    calendar.add(Calendar.YEAR, 1);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND,0);
    Instant expected = calendar.getTime().toInstant();

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setCronExpression("0 0 0 1 1 ? *");
    scheduler.setStartDateTime(currentTime);
    Jobs the_job = new Jobs();
    scheduler.setJob(the_job);
    JobConfiguration jobConfig = new SparkJobConfiguration();
    the_job.setJobConfig(jobConfig);

    ArrayList<JobScheduleV2> schedulers = new ArrayList<>();
    schedulers.add(scheduler);

    Mockito.when(executionFacade.findByJobAndNotFinished(any(Jobs.class))).thenReturn(new ArrayList<>());
    Mockito.when(jobScheduleV2Facade.getActiveWithCurrentExecutionTime(any())).thenReturn(schedulers);
    Mockito.when(jobScheduleV2Facade.update(any(JobScheduleV2.class))).thenReturn(scheduler);
    ArgumentCaptor<JobScheduleV2> argumentCaptor = ArgumentCaptor.forClass(JobScheduleV2.class);
    
    target.executeWithCron(currentTime);
    verify(jobScheduleV2Facade, times(1)).update(argumentCaptor.capture());

    JobScheduleV2 updatedSchedule = argumentCaptor.getValue();
    Assert.assertEquals(expected, updatedSchedule.getNextExecutionDateTime());
  }

  // test that the scheduler doesn't launch multiple executions for the same job
  @Test
  public void testExecuteWithCronNoParallel()
          throws JobException, ProjectException, ServiceException, GenericException {
    Instant currentTime = Instant.now();

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setCronExpression("0 0 0 1 1 ? *");
    scheduler.setStartDateTime(currentTime);
    Jobs the_job = new Jobs();
    scheduler.setJob(the_job);
    JobConfiguration jobConfig = new SparkJobConfiguration();
    the_job.setJobConfig(jobConfig);

    ArrayList<JobScheduleV2> schedulers = new ArrayList<>();
    schedulers.add(scheduler);

    List<Execution> existingExecutions = new ArrayList<>();
    existingExecutions.add(new Execution());
    Mockito.when(executionFacade.findByJobAndNotFinished(any(Jobs.class))).thenReturn(existingExecutions);

    Mockito.when(jobScheduleV2Facade.getActiveWithCurrentExecutionTime(any())).thenReturn(schedulers);
    // Exception should not be thrown as the method should not be invoked.
    Mockito.when(jobScheduleV2Facade.update(any(JobScheduleV2.class))).thenThrow(new IllegalArgumentException());

    target.executeWithCron(currentTime);
  }

  @Test
  public void testEnableSchedulerWithPreviousExecutions() throws JobException {
    Calendar previousExecutionDateTime = Calendar.getInstance();
    previousExecutionDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    previousExecutionDateTime.set(2021, 1, 1, 0, 10, 0);


    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setEnabled(false);
    scheduler.setCronExpression("0 */10 * ? * * *");
    scheduler.setNextExecutionDateTime(previousExecutionDateTime.toInstant());

    Mockito.when(jobScheduleV2Facade.getById(any())).thenReturn(Optional.of(scheduler));
    Mockito.when(jobScheduleV2Facade.update(any(JobScheduleV2.class))).thenReturn(scheduler);
    ArgumentCaptor<JobScheduleV2> argumentCaptor = ArgumentCaptor.forClass(JobScheduleV2.class);

    JobScheduleV2DTO updatedJobScheduleDTO = new JobScheduleV2DTO();
    updatedJobScheduleDTO.setEnabled(true);
    updatedJobScheduleDTO.setCronExpression("0 */10 * ? * * *");
    target.updateSchedule(updatedJobScheduleDTO);
    verify(jobScheduleV2Facade, times(1)).update(argumentCaptor.capture());

    JobScheduleV2 capturedSchedule = argumentCaptor.getValue();
    Assert.assertEquals(true, capturedSchedule.getEnabled());

    Calendar nextExecutionDateCalendar = Calendar.getInstance();
    nextExecutionDateCalendar.setTime(Date.from(capturedSchedule.getNextExecutionDateTime()));
    Assert.assertEquals(20, nextExecutionDateCalendar.get(Calendar.MINUTE));
  }

  @Test
  public void testEnableSchedulerWithPreviousExecutionPriorToNewStartDateTime() throws JobException {
    Calendar previousExecutionDateTime = Calendar.getInstance();
    previousExecutionDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    previousExecutionDateTime.set(2021, 1, 1, 0, 10, 0);

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setEnabled(false);
    scheduler.setCronExpression("0 */10 * ? * * *");
    scheduler.setNextExecutionDateTime(previousExecutionDateTime.toInstant());

    Mockito.when(jobScheduleV2Facade.getById(any())).thenReturn(Optional.of(scheduler));
    Mockito.when(jobScheduleV2Facade.update(any(JobScheduleV2.class))).thenReturn(scheduler);
    ArgumentCaptor<JobScheduleV2> argumentCaptor = ArgumentCaptor.forClass(JobScheduleV2.class);

    Calendar newStartDateTime = Calendar.getInstance();
    newStartDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    newStartDateTime.set(2021, 1, 1, 0, 30, 0);

    JobScheduleV2DTO updatedJobScheduleDTO = new JobScheduleV2DTO();
    updatedJobScheduleDTO.setEnabled(true);
    updatedJobScheduleDTO.setStartDateTime(newStartDateTime.toInstant());
    updatedJobScheduleDTO.setCronExpression("0 */10 * ? * * *");
    target.updateSchedule(updatedJobScheduleDTO);

    verify(jobScheduleV2Facade, times(1)).update(argumentCaptor.capture());

    JobScheduleV2 capturedSchedule = argumentCaptor.getValue();
    Assert.assertEquals(true, capturedSchedule.getEnabled());

    Calendar nextExecutionDateCalendar = Calendar.getInstance();
    nextExecutionDateCalendar.setTime(Date.from(capturedSchedule.getNextExecutionDateTime()));
    Assert.assertEquals(40, nextExecutionDateCalendar.get(Calendar.MINUTE));
  }

  @Test
  public void testEnableSchedulerWithNoPreviousExecutions() throws JobException {
    Calendar startDateTime = Calendar.getInstance();
    startDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    startDateTime.set(2021, 1, 1, 0, 10, 0);


    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setEnabled(false);
    scheduler.setStartDateTime(startDateTime.toInstant());
    scheduler.setCronExpression("0 */10 * ? * * *");

    Mockito.when(jobScheduleV2Facade.getById(any())).thenReturn(Optional.of(scheduler));
    Mockito.when(jobScheduleV2Facade.update(any(JobScheduleV2.class))).thenReturn(scheduler);
    ArgumentCaptor<JobScheduleV2> argumentCaptor = ArgumentCaptor.forClass(JobScheduleV2.class);

    JobScheduleV2DTO updatedJobScheduleDTO = new JobScheduleV2DTO();
    updatedJobScheduleDTO.setEnabled(true);
    updatedJobScheduleDTO.setStartDateTime(startDateTime.toInstant());
    updatedJobScheduleDTO.setCronExpression("0 */10 * ? * * *");
    target.updateSchedule(updatedJobScheduleDTO);

    verify(jobScheduleV2Facade, times(1)).update(argumentCaptor.capture());

    JobScheduleV2 capturedSchedule = argumentCaptor.getValue();
    Assert.assertEquals(true, capturedSchedule.getEnabled());

    Calendar nextExecutionDateCalendar = Calendar.getInstance();
    nextExecutionDateCalendar.setTime(Date.from(capturedSchedule.getNextExecutionDateTime()));
    Assert.assertEquals(20, nextExecutionDateCalendar.get(Calendar.MINUTE));
  }

  @Test
  public void testDisableScheduler() throws JobException {
    Instant nextExecutionTime = Instant.now();

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setEnabled(true);
    scheduler.setCronExpression("0 0 0 ? * 1 *");
    scheduler.setNextExecutionDateTime(nextExecutionTime);

    Mockito.when(jobScheduleV2Facade.getById(any())).thenReturn(Optional.of(scheduler));
    Mockito.when(jobScheduleV2Facade.update(any(JobScheduleV2.class))).thenReturn(scheduler);
    ArgumentCaptor<JobScheduleV2> argumentCaptor = ArgumentCaptor.forClass(JobScheduleV2.class);

    JobScheduleV2DTO updatedJobScheduleDTO = new JobScheduleV2DTO();
    updatedJobScheduleDTO.setEnabled(false);
    updatedJobScheduleDTO.setCronExpression("0 0 0 ? * 1 *");
    target.updateSchedule(updatedJobScheduleDTO);

    verify(jobScheduleV2Facade, times(1)).update(argumentCaptor.capture());

    JobScheduleV2 capturedSchedule = argumentCaptor.getValue();
    Assert.assertEquals(false, capturedSchedule.getEnabled());
    Assert.assertEquals(nextExecutionTime, capturedSchedule.getNextExecutionDateTime());
  }

  @Test
  public void testUpdateCronExpression() throws JobException {
    Instant nextExecutionTime = Instant.now();

    JobScheduleV2 scheduler = new JobScheduleV2();
    scheduler.setEnabled(true);
    scheduler.setCronExpression("0 0 0 ? * 1 *");
    scheduler.setNextExecutionDateTime(nextExecutionTime);

    Mockito.when(jobScheduleV2Facade.getById(any())).thenReturn(Optional.of(scheduler));
    Mockito.when(jobScheduleV2Facade.update(any(JobScheduleV2.class))).thenReturn(scheduler);
    ArgumentCaptor<JobScheduleV2> argumentCaptor = ArgumentCaptor.forClass(JobScheduleV2.class);

    String updatedCronExpression = "0 */5 */5 ? * 1 *";
    JobScheduleV2DTO updatedJobScheduleDTO = new JobScheduleV2DTO();
    updatedJobScheduleDTO.setCronExpression(updatedCronExpression);
    updatedJobScheduleDTO.setEnabled(true);
    target.updateSchedule(updatedJobScheduleDTO);

    verify(jobScheduleV2Facade, times(1)).update(argumentCaptor.capture());

    JobScheduleV2 capturedSchedule = argumentCaptor.getValue();
    Assert.assertEquals(updatedCronExpression, capturedSchedule.getCronExpression());
  }
}