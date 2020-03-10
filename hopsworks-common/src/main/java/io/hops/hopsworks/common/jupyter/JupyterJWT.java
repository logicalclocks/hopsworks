/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.jupyter;

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.nio.file.Path;
import java.time.LocalDateTime;

public final class JupyterJWT {
  public final Project project;
  public final Users user;
  public final PidAndPort pidAndPort;
  public final LocalDateTime expiration;
  public Path tokenFile;
  public String token;
  
  public JupyterJWT(JupyterJWT jupyterJWT) {
    this(jupyterJWT.project, jupyterJWT.user, jupyterJWT.pidAndPort, jupyterJWT.expiration);
    this.tokenFile = jupyterJWT.tokenFile;
  }
  
  public JupyterJWT(Project project, Users user, PidAndPort pidAndPort, LocalDateTime expiration) {
    this.project = project;
    this.user = user;
    this.pidAndPort = pidAndPort;
    this.expiration = expiration;
  }
  
  public boolean maybeRenew(LocalDateTime now) {
    return now.isAfter(expiration) || now.isEqual(expiration);
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + project.getId();
    result = 31 * result + user.getUid();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o instanceof JupyterJWT) {
      JupyterJWT other = (JupyterJWT) o;
      return user.getUid().equals(other.user.getUid()) && project.getId().equals(other.project.getId());
    }
    return false;
  }
  
  @Override
  public String toString() {
    return "(" + project.getName() + "/" + user.getUsername() + ")";
  }
}
