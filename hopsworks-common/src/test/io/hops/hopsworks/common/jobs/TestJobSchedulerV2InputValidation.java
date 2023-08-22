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

import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2Controller;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2DTO;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2InputValidation;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertThrows;


public class TestJobSchedulerV2InputValidation {
  @InjectMocks
  private JobScheduleV2InputValidation validator = new JobScheduleV2InputValidation();
  @Mock
  private JobScheduleV2Controller jobScheduleV2Controller;

  private Jobs job;
  private final String jobName = "the_job";


  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    job = new Jobs();
    job.setName(jobName);
    
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    Mockito.when(jobScheduleV2Controller
            .computeNextExecutionDateTime(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
    ).thenReturn(calendar.getTime().toInstant());
  }

  @Test
  public void testCronValidationNoSeconds() throws JobException {
    JobScheduleV2DTO dto = new JobScheduleV2DTO();

    dto.setCronExpression("1 */10 0 1 * ? *");
    assertThrows(
      IllegalArgumentException.class,
      () -> validator.validateJobScheduleDTO(dto)
    );
  }

  @Test
  public void testCronValidationStandardOptions() throws JobException {
    JobScheduleV2DTO dto = new JobScheduleV2DTO();
    dto.setStartDateTime(Instant.now());

    // Daily at midnight
    dto.setCronExpression("0 0 0 * * ? *");
    validator.validateJobScheduleDTO(dto);

    // Monthly at midnight
    dto.setCronExpression("0 0 0 1 * ? *");
    validator.validateJobScheduleDTO(dto);

    // Weekly at 8:30
    dto.setCronExpression("0 30 8 ? * MON *");
    validator.validateJobScheduleDTO(dto);

    // Every 10 minutes, near real-time
    dto.setCronExpression("0 */10 * * * ? *");
    validator.validateJobScheduleDTO(dto);

    // Hourly, at 30 minutes past the hour
    dto.setCronExpression("0 30 * * * ? *");
    validator.validateJobScheduleDTO(dto);

    // Every 30min
    dto.setCronExpression("0 0,30/30 * * * ? *");
    validator.validateJobScheduleDTO(dto);

    // Every weekday at 2am
    dto.setCronExpression("0 0 2 ? * MON-FRI *");
    validator.validateJobScheduleDTO(dto);

    // Every 4 hours, on weekends
    dto.setCronExpression("0 0 */4 ? * SAT,SUN *");
    validator.validateJobScheduleDTO(dto);
  }
}
