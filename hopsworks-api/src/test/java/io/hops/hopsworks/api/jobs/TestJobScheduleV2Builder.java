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

package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.jobs.scheduler.JobScheduleV2Builder;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2Controller;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2DTO;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2InputValidation;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestJobScheduleV2Builder {
  @InjectMocks
  private JobScheduleV2Builder target = new JobScheduleV2Builder();
  @Mock
  private JobScheduleV2InputValidation inputValidation;
  @Mock
  private JobScheduleV2Controller jobScheduleV2Controller;
  private Calendar calendar;

  private Boolean isDtoEqualToPojo(JobScheduleV2DTO dto, JobScheduleV2 pojo) {
    if (dto.getId() != null && !dto.getId().equals(pojo.getId())) {
      return false;
    }
    return dto.getCronExpression().equals(pojo.getCronExpression())
        && dto.getStartDateTime().equals(pojo.getStartDateTime())
        && dto.getEndDateTime().equals(pojo.getEndDateTime())
        && dto.getEnabled().equals(pojo.getEnabled());
  }

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @Test
  public void testConvertJobScheduleDTO() throws Exception {
    Jobs job = new Jobs();

    job.setId(1);
    job.setName("test_job");
    JobScheduleV2DTO scheduleDTO = new JobScheduleV2DTO();
    scheduleDTO.setEnabled(true);
    scheduleDTO.setCronExpression("10 0 * ? * * *");

    calendar.set(2021, Calendar.MARCH, 1, 0, 0, 0);
    scheduleDTO.setStartDateTime(calendar.getTime().toInstant());
    calendar.set(2023, Calendar.MAY, 1, 0, 0, 0);
    scheduleDTO.setEndDateTime(calendar.getTime().toInstant());

    JobScheduleV2 jobSchedule = target.convertJobScheduleDTO(job, scheduleDTO);
    Assert.assertTrue(isDtoEqualToPojo(scheduleDTO, jobSchedule));

    jobSchedule = target.convertJobScheduleDTO(job, scheduleDTO);
    Assert.assertTrue(isDtoEqualToPojo(scheduleDTO, jobSchedule));
  }
}