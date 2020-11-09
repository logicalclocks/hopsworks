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
package io.hops.hopsworks.api.provenance.ops.dto;

import io.hops.hopsworks.api.jobs.JobDTO;
import io.hops.hopsworks.api.jobs.executions.ExecutionDTO;
import io.hops.hopsworks.api.user.UserDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ProvArtifactUsageDTO {
  private Long timestamp;
  private String readableTimestamp;
  private ExecutionDTO execution;
  private JobDTO job;
  private UserDTO user;
  
  public ProvArtifactUsageDTO() {
  }
  
  public Long getTimestamp() {
    return timestamp;
  }
  
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }
  
  public String getReadableTimestamp() {
    return readableTimestamp;
  }
  
  public void setReadableTimestamp(String readableTimestamp) {
    this.readableTimestamp = readableTimestamp;
  }
  
  public JobDTO getJob() {
    return job;
  }
  
  public void setJob(JobDTO job) {
    this.job = job;
  }
  
  public ExecutionDTO getExecution() {
    return execution;
  }
  
  public void setExecution(ExecutionDTO execution) {
    this.execution = execution;
  }
  
  public UserDTO getUser() {
    return user;
  }
  
  public void setUser(UserDTO user) {
    this.user = user;
  }
}
