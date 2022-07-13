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
package io.hops.hopsworks.persistence.entity.project.alert;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidationStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;

public enum ProjectServiceAlertStatus {
  //Feature group
  VALIDATION_SUCCESS("Success"),
  VALIDATION_WARNING("Warning"),
  VALIDATION_FAILURE("Failure"),
  //Job
  JOB_FINISHED("Finished"),
  JOB_FAILED("Failed"),
  JOB_KILLED("Killed");

  private final String name;

  ProjectServiceAlertStatus(String name) {
    this.name = name;
  }

  public static ProjectServiceAlertStatus fromString(String name) {
    return valueOf(name.toUpperCase());
  }

  public String getName() {
    return name;
  }

  public static ProjectServiceAlertStatus getStatus(FeatureGroupValidationStatus status) {
    switch (status) {
      case FAILURE:
        return ProjectServiceAlertStatus.VALIDATION_FAILURE;
      case SUCCESS:
        return ProjectServiceAlertStatus.VALIDATION_SUCCESS;
      case WARNING:
        return ProjectServiceAlertStatus.VALIDATION_WARNING;
      default:
        throw new IllegalArgumentException("Invalid enum constant");//will happen if status is none
    }
  }

  public static ProjectServiceAlertStatus getJobAlertStatus(JobState jobState) {
    switch (jobState) {
      case FINISHED:
        return ProjectServiceAlertStatus.JOB_FINISHED;
      case FAILED:
      case FRAMEWORK_FAILURE:
      case APP_MASTER_START_FAILED:
      case INITIALIZATION_FAILED:
        return ProjectServiceAlertStatus.JOB_FAILED;
      case KILLED:
        return ProjectServiceAlertStatus.JOB_KILLED;
      default:
        throw new IllegalArgumentException("Invalid enum constant");//will happen if JobState is not final state
    }
  }

  public static ProjectServiceAlertStatus getJobAlertStatus(JobFinalStatus jobFinalStatus) {
    switch (jobFinalStatus) {
      case SUCCEEDED:
        return ProjectServiceAlertStatus.JOB_FINISHED;
      case FAILED:
        return ProjectServiceAlertStatus.JOB_FAILED;
      case KILLED:
        return ProjectServiceAlertStatus.JOB_KILLED;
      default:
        throw new IllegalArgumentException("Invalid enum constant");//will happen if JobState is not final state
    }
  }

  public boolean isJobStatus() {
    switch (this) {
      case JOB_FAILED:
      case JOB_KILLED:
      case JOB_FINISHED:
        return true;
      default:
        return false;
    }
  }

  public boolean isFeatureGroupStatus() {
    switch (this) {
      case VALIDATION_FAILURE:
      case VALIDATION_SUCCESS:
      case VALIDATION_WARNING:
        return true;
      default:
        return false;
    }
  }

  @Override
  public String toString() {
    return name;
  }
}
