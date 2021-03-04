/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.activities;

import io.hops.hopsworks.api.featurestore.commit.CommitDTO;
import io.hops.hopsworks.api.featurestore.datavalidation.validations.FeatureGroupValidationDTO;
import io.hops.hopsworks.api.featurestore.statistics.StatisticsDTO;
import io.hops.hopsworks.api.jobs.JobDTO;
import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.featurestore.activity.ActivityType;

public class ActivityDTO extends RestDTO<ActivityDTO> {

  private ActivityType type;
  private Long timestamp;
  private UserDTO user;

  private String metadata;
  private JobDTO job;
  private StatisticsDTO statistics;
  private CommitDTO commit;
  private FeatureGroupValidationDTO validations;

  public ActivityDTO() { }

  public ActivityType getType() {
    return type;
  }

  public void setType(ActivityType type) {
    this.type = type;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public UserDTO getUser() {
    return user;
  }

  public void setUser(UserDTO user) {
    this.user = user;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public JobDTO getJob() {
    return job;
  }

  public void setJob(JobDTO job) {
    this.job = job;
  }

  public StatisticsDTO getStatistics() {
    return statistics;
  }

  public void setStatistics(StatisticsDTO statistics) {
    this.statistics = statistics;
  }

  public CommitDTO getCommit() {
    return commit;
  }

  public void setCommit(CommitDTO commit) {
    this.commit = commit;
  }

  public FeatureGroupValidationDTO getValidations() {
    return validations;
  }

  public void setValidations(FeatureGroupValidationDTO validations) {
    this.validations = validations;
  }
}
