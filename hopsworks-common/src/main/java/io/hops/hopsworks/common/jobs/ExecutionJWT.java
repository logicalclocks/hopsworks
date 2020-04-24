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

package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.jwt.ServiceJWT;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.time.LocalDateTime;

public class ExecutionJWT extends ServiceJWT {
  
  public final Execution execution;
  
  public ExecutionJWT(ExecutionJWT executionJWT) {
    super(executionJWT);
    this.execution = executionJWT.execution;
  }
  
  public ExecutionJWT(Project project, Users user, Execution execution,
    LocalDateTime expiration) {
    super(project, user, expiration);
    this.execution = execution;
  }
  
  public ExecutionJWT(Execution execution) {
    super(execution.getJob().getProject(), execution.getUser(), null);
    this.execution = execution;
  }
  
  public ExecutionJWT(Execution execution, String token) {
    super(execution.getJob().getProject(), execution.getUser(), null);
    this.execution = execution;
    super.token = token;
  }
  
  public ExecutionJWT(Execution execution, LocalDateTime expiration) {
    super(execution.getJob().getProject(), execution.getUser(), expiration);
    this.execution = execution;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    
    if (o instanceof ExecutionJWT) {
      ExecutionJWT other = (ExecutionJWT) o;
      return execution.getId().equals(other.execution.getId());
    }
    return false;
  }
  
  @Override
  public String toString() {
    return "(" + execution.getJob().getName() + "/" + execution.getId() + ")";
  }
}
