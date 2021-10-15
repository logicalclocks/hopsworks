/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.jobs.description;

import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum JobAlertStatus {

  FINISHED("Finished"),
  FAILED("Failed"),
  KILLED("Killed");

  private final String name;

  JobAlertStatus(String name) {
    this.name = name;
  }

  public static JobAlertStatus fromString(String name) {
    return valueOf(name.toUpperCase());
  }

  public String getName() {
    return name;
  }

  public static JobAlertStatus getJobAlertStatus(JobState jobState) {
    switch (jobState) {
      case FINISHED:
        return JobAlertStatus.FINISHED;
      case FAILED:
      case FRAMEWORK_FAILURE:
      case APP_MASTER_START_FAILED:
      case INITIALIZATION_FAILED:
        return JobAlertStatus.FAILED;
      case KILLED:
        return JobAlertStatus.KILLED;
      default:
        throw new IllegalArgumentException("Invalid enum constant");//will happen if JobState is not final state
    }
  }

  public static JobAlertStatus getJobAlertStatus(JobFinalStatus jobFinalStatus) {
    switch (jobFinalStatus) {
      case SUCCEEDED:
        return JobAlertStatus.FINISHED;
      case FAILED:
        return JobAlertStatus.FAILED;
      case KILLED:
        return JobAlertStatus.KILLED;
      default:
        throw new IllegalArgumentException("Invalid enum constant");//will happen if JobState is not final state
    }
  }
}
