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
package io.hops.hopsworks.api.dataset.inode;

import io.hops.hopsworks.persistence.entity.command.CommandStatus;
import io.hops.hopsworks.persistence.entity.hdfs.command.Command;
import io.hops.hopsworks.persistence.entity.hdfs.command.HdfsCommandExecution;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;

import java.util.Date;

public class HdfsCommandDTO {
  private Integer id;
  private Integer projectId;
  private CommandStatus status;
  private Command command;
  private Integer executionId;
  private Date submitted;
  private String srcPath;
  private String jobName;
  
  public HdfsCommandDTO() {
  }
  
  public HdfsCommandDTO(HdfsCommandExecution hdfsCommandExecution, String srcPath) {
    this.id = hdfsCommandExecution.getId();
    this.projectId = hdfsCommandExecution.getExecution().getJob() != null ?
      hdfsCommandExecution.getExecution().getJob().getProject().getId() : null;
    this.status = fromExecutionStatus(hdfsCommandExecution.getExecution());
    this.command = hdfsCommandExecution.getCommand();
    this.executionId = hdfsCommandExecution.getExecution().getId();
    this.submitted = hdfsCommandExecution.getSubmitted();
    this.srcPath = srcPath;
    this.jobName = hdfsCommandExecution.getExecution().getJob() != null ?
      hdfsCommandExecution.getExecution().getJob().getName() : null;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public CommandStatus getStatus() {
    return status;
  }
  
  public void setStatus(CommandStatus status) {
    this.status = status;
  }
  
  public Command getCommand() {
    return command;
  }
  
  public void setCommand(Command command) {
    this.command = command;
  }
  
  public Integer getExecutionId() {
    return executionId;
  }
  
  public void setExecutionId(Integer executionId) {
    this.executionId = executionId;
  }
  
  public Date getSubmitted() {
    return submitted;
  }
  
  public void setSubmitted(Date submitted) {
    this.submitted = submitted;
  }
  
  public String getSrcPath() {
    return srcPath;
  }
  
  public void setSrcPath(String srcPath) {
    this.srcPath = srcPath;
  }
  
  public String getJobName() {
    return jobName;
  }
  
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }
  
  private CommandStatus fromExecutionStatus(Execution execution) {
    if (execution.getFinalStatus() != null && !JobFinalStatus.UNDEFINED.equals(execution.getFinalStatus())) {
      switch (execution.getFinalStatus()) {
        case KILLED:
        case FAILED:
          return CommandStatus.FAILED;
        default:
          return CommandStatus.FINISHED;
      }
    }
    switch (execution.getState()) {
      case FAILED:
      case KILLED:
      case FRAMEWORK_FAILURE:
      case APP_MASTER_START_FAILED:
      case INITIALIZATION_FAILED:
        return CommandStatus.FAILED;
      case INITIALIZING:
      case NEW:
      case NEW_SAVING:
      case SUBMITTED:
      case STARTING_APP_MASTER:
        return CommandStatus.NEW;
      case RUNNING:
      case ACCEPTED:
      case AGGREGATING_LOGS:
      case GENERATING_SECURITY_MATERIAL:
        return CommandStatus.ONGOING;
      default:
        return CommandStatus.FINISHED;
    }
  }
}
