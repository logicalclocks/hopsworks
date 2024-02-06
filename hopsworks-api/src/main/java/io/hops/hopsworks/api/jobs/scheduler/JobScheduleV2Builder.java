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

package io.hops.hopsworks.api.jobs.scheduler;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2DTO;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2InputValidation;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobScheduleV2Builder {
  @EJB
  private JobScheduleV2InputValidation inputValidation;

  public JobScheduleV2 validateAndConvertOnCreate(Jobs job, JobScheduleV2DTO scheduleDTO) {
    inputValidation.validateJobScheduleDTO(scheduleDTO);
    return convertJobScheduleDTO(job, scheduleDTO);
  }

  public JobScheduleV2 validateAndConvertOnUpdate(Jobs job, JobScheduleV2DTO scheduleDTO)
      throws IllegalArgumentException {
    if (scheduleDTO.getId() == null) {
      // Need to specify schedule id because there can be multiple schedules linked to a job in the future.
      throw new IllegalArgumentException(String.format(
              "Cannot update schedule for job %s  because the job schedule id is missing.",
              job.getName()));
    }
    inputValidation.validateJobScheduleDTO(scheduleDTO);
    return convertJobScheduleDTO(job, scheduleDTO);
  }

  public JobScheduleV2 convertJobScheduleDTO(Jobs job, JobScheduleV2DTO scheduleDTO) throws IllegalArgumentException {
    return new JobScheduleV2(
        scheduleDTO.getId(),
        job,
        scheduleDTO.getStartDateTime(),
        scheduleDTO.getEndDateTime(),
        scheduleDTO.getEnabled(),
        scheduleDTO.getCronExpression(),
        scheduleDTO.getNextExecutionDateTime()
    );
  }

  public URI uri(Jobs job, UriInfo uriInfo) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
            .path(Integer.toString(job.getProject().getId()))
            .path(ResourceRequest.Name.JOBS.toString().toLowerCase())
            .path(job.getName())
            .path(ResourceRequest.Name.SCHEDULE.toString().toLowerCase())
            .path("v2") // TODO: Remove in subsequent PR
            .build();
  }

  public JobScheduleV2DTO build(UriInfo uriInfo, JobScheduleV2 schedule) {
    if (schedule == null) { return null; }

    JobScheduleV2DTO jobScheduleV2DTO = new JobScheduleV2DTO();
    jobScheduleV2DTO.setHref(uri(schedule.getJob(), uriInfo));
    jobScheduleV2DTO.setId(schedule.getId());
    jobScheduleV2DTO.setStartDateTime(schedule.getStartDateTime());
    jobScheduleV2DTO.setEndDateTime(schedule.getEndDateTime());
    jobScheduleV2DTO.setEnabled(schedule.getEnabled());
    jobScheduleV2DTO.setCronExpression(schedule.getCronExpression());
    jobScheduleV2DTO.setNextExecutionDateTime(schedule.getNextExecutionDateTime());

    return jobScheduleV2DTO;
  }
}
