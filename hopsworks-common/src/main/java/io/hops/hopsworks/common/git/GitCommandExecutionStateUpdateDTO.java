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
package io.hops.hopsworks.common.git;

import io.hops.hopsworks.persistence.entity.git.config.GitOpExecutionState;

public class GitCommandExecutionStateUpdateDTO {
  private GitOpExecutionState executionState;
  private String message;
  private String branch;
  private String commitHash;

  public GitCommandExecutionStateUpdateDTO() {}

  public GitOpExecutionState getExecutionState() { return executionState; }

  public void setExecutionState(GitOpExecutionState executionState) { this.executionState = executionState; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public String getBranch() { return branch; }

  public void setBranch(String branch) { this.branch = branch; }

  public String getCommitHash() { return commitHash; }

  public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
}
