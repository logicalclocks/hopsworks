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
package io.hops.hopsworks.api.git.execution;

import io.hops.hopsworks.api.git.repository.GitRepositoryDTO;
import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;
import io.hops.hopsworks.persistence.entity.git.config.GitOpExecutionState;
import java.util.Date;

public class GitOpExecutionDTO extends RestDTO<GitOpExecutionDTO> {
  private Integer id;
  private Date submissionTime;
  private long executionStart;
  private long executionStop;
  private UserDTO user;
  private GitCommandConfiguration gitCommandConfiguration;
  private GitOpExecutionState state;
  private String commandResultMessage;
  private String configSecret;
  private GitRepositoryDTO repository;

  public Integer getId() { return id; }

  public void setId(Integer id) {this.id = id; }

  public Date getSubmissionTime() { return submissionTime; }

  public void setSubmissionTime(Date submissionTime) { this.submissionTime = submissionTime; }

  public long getExecutionStart() { return executionStart; }

  public void setExecutionStart(long executionStart) { this.executionStart = executionStart; }

  public long getExecutionStop() { return executionStop; }

  public void setExecutionStop(long executionStop) { this.executionStop = executionStop; }

  public UserDTO getUser() { return user; }

  public void setUser(UserDTO user) { this.user = user; }

  public GitCommandConfiguration getGitCommandConfiguration() { return gitCommandConfiguration; }

  public void setGitCommandConfiguration(GitCommandConfiguration gitCommandConfiguration) {
    this.gitCommandConfiguration = gitCommandConfiguration;
  }

  public GitOpExecutionState getState() { return state; }

  public void setState(GitOpExecutionState state) { this.state = state; }

  public String getCommandResultMessage() { return commandResultMessage; }

  public void setCommandResultMessage(String commandResultMessage) { this.commandResultMessage = commandResultMessage; }

  public String getConfigSecret() { return configSecret; }

  public void setConfigSecret(String configSecret) { this.configSecret = configSecret; }

  public GitRepositoryDTO getRepository() { return repository; }

  public void setRepository(GitRepositoryDTO repository) { this.repository = repository; }
}
