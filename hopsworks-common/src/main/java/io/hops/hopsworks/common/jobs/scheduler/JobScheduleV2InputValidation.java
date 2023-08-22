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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.time.Instant;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import static com.cronutils.model.CronType.QUARTZ;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobScheduleV2InputValidation {
  @EJB
  private JobScheduleV2Controller jobScheduleV2Controller;

  public void validateJobScheduleDTO(JobScheduleV2DTO schedulerDTO) {
    // Validate schedule times
    validateScheduleTimes(schedulerDTO);
    
    // Parse cron expression to validate it.
    validateCronExpression(schedulerDTO.getCronExpression());
  }

  private void validateCronExpression(String cronExpression) throws IllegalArgumentException {
    CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
    CronParser parser = new CronParser(cronDefinition);

    Cron parsedCron;
    try {
      parsedCron = parser.parse(cronExpression);
      parsedCron.validate();
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(String.format("Invalid cron expression provided '%s'.", cronExpression));
    }

    enforceZeroSecondsInCronExpression(cronExpression);
  }

  private void enforceZeroSecondsInCronExpression(String cronExpression) {
    // This function assumes cronExpression is a valid cron string.
    String secondsCron = cronExpression.split(" ")[0];
    if (!secondsCron.equals("0")) {
      throw new IllegalArgumentException(String.format("Invalid cron expression %s provided. Seconds field '%s'."
          + " Seconds should be 0.", cronExpression, secondsCron));
    }
  }
  private void validateScheduleTimes(JobScheduleV2DTO scheduleDTO) {
    Instant currentTime = Instant.now();
    if (scheduleDTO.getCronExpression() == null) {
      throw new IllegalArgumentException(String.format("Cron expression is required in job schedules."));
    }
    if (jobScheduleV2Controller.computeNextExecutionDateTime(currentTime, null,
            scheduleDTO.getStartDateTime(), scheduleDTO.getEndDateTime(), scheduleDTO.getCronExpression()) == null) {
      throw new IllegalArgumentException(String.format("Invalid job schedule. The next execution time has already " +
        "passed and will never be triggered"));
    }
  }
}
