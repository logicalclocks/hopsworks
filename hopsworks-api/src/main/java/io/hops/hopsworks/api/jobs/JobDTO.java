/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.api.jobs.executions.ExecutionDTO;
import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class JobDTO extends RestDTO<JobDTO> {
  
  private Integer id;
  private String name;
  private Date creationTime;
  private JobConfiguration config;
  private JobType jobType;
  private UserDTO creator;
  private ExecutionDTO executions;
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Date getCreationTime() {
    return creationTime;
  }
  
  public void setCreationTime(Date creationTime) {
    this.creationTime = creationTime;
  }
  
  public JobConfiguration getConfig() {
    return config;
  }
  
  public void setConfig(JobConfiguration config) {
    this.config = config;
  }
  
  public JobType getJobType() {
    return jobType;
  }
  
  public void setJobType(JobType jobType) {
    this.jobType = jobType;
  }
  
  public UserDTO getCreator() {
    return creator;
  }
  
  public void setCreator(UserDTO creator) {
    this.creator = creator;
  }
  
  public ExecutionDTO getExecutions() {
    return executions;
  }
  
  public void setExecutions(ExecutionDTO executions) {
    this.executions = executions;
  }
  
}