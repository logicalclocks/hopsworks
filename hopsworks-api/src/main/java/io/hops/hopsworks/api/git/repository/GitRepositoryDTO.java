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
package io.hops.hopsworks.api.git.repository;

import io.hops.hopsworks.api.git.execution.GitOpExecutionDTO;
import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.git.GitCommitDTO;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GitRepositoryDTO extends RestDTO<GitRepositoryDTO> {
  private Integer id;
  private String name;
  private String path;
  private UserDTO creator;
  private GitProvider provider;
  private String currentBranch;
  private GitCommitDTO currentCommit;
  private GitOpExecutionDTO ongoingOperation;
  private Boolean readOnly;

  public GitRepositoryDTO() {}

  public GitRepositoryDTO(Integer id, String name, String path, UserDTO creator, GitProvider provider,
                          String currentBranch, GitCommitDTO currentCommitHash, Boolean readOnly) {
    this.id = id;
    this.name = name;
    this.path = path;
    this.creator = creator;
    this.provider = provider;
    this.currentBranch = currentBranch;
    this.currentCommit = currentCommitHash;
    this.readOnly = readOnly;
  }

  public Integer getId() { return id; }

  public void setId(Integer id) { this.id = id; }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() { return path; }

  public void setPath(String path) { this.path = path; }

  public UserDTO getCreator() { return creator; }

  public void setCreator(UserDTO creator) { this.creator = creator; }

  public GitProvider getProvider() { return provider; }

  public void setProvider(GitProvider provider) { this.provider = provider; }

  public GitOpExecutionDTO getOngoingOperation() { return ongoingOperation; }

  public void setOngoingOperation(GitOpExecutionDTO ongoingOperation) { this.ongoingOperation = ongoingOperation; }

  public String getCurrentBranch() { return currentBranch; }

  public void setCurrentBranch(String currentBranch) { this.currentBranch = currentBranch; }

  public GitCommitDTO getCurrentCommit() { return currentCommit; }

  public void setCurrentCommit(GitCommitDTO currentCommit) { this.currentCommit = currentCommit; }

  public Boolean getReadOnly() { return readOnly; }

  public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }

  @Override
  public String toString() {
    return "GitRepositoryDTO{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", path='" + path + '\'' +
        ", creator=" + creator +
        ", provider=" + provider +
        ", currentBranch='" + currentBranch + '\'' +
        ", currentCommit=" + currentCommit +
        ", ongoingOperation=" + ongoingOperation +
        ", readonly=" + readOnly +
        '}';
  }
}
