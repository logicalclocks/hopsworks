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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hops.hopsworks.common.api.RestDTO;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitCommitDTO extends RestDTO<GitCommitDTO> {
  private String name;
  private String email;
  private String message;
  private String commitHash;
  private Date time;

  public GitCommitDTO() {}

  public GitCommitDTO(String name, String email, String message, String commitHash, Date time) {
    this.name = name;
    this.email = email;
    this.message = message;
    this.commitHash = commitHash;
    this.time = time;
  }

  public String getName() { return name; }

  public void setName(String name) { this.name = name; }

  public String getEmail() { return email; }

  public void setEmail(String email) { this.email = email; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public String getCommitHash() { return commitHash; }

  public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

  public Date getTime() { return time; }

  public void setTime(Date time) { this.time = time; }

  @Override
  public String toString() {
    return "GitCommitDTO{" +
        "name='" + name + '\'' +
        ", email='" + email + '\'' +
        ", message='" + message + '\'' +
        ", commitHash='" + commitHash + '\'' +
        ", time=" + time +
        '}';
  }
}
