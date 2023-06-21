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
package io.hops.hopsworks.common.jupyter;

import io.hops.hopsworks.common.util.DateUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class JupyterJWTDTO implements Serializable {
  private static final long serialVersionUID = -5687462769985361531L;
  private Integer projectId;
  private Integer userId;
  private LocalDateTime expiration;
  private String token;
  private String tokenFile;
  private final CidAndPort pidAndPort;
  
  public JupyterJWTDTO(JupyterJWT jupyterJWT) {
    this.projectId = jupyterJWT.project.getId();
    this.userId = jupyterJWT.user.getUid();
    this.expiration = jupyterJWT.expiration;
    this.token = jupyterJWT.token;
    this.tokenFile = jupyterJWT.tokenFile.toString();
    this.pidAndPort = jupyterJWT.pidAndPort;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public Integer getUserId() {
    return userId;
  }
  
  public void setUserId(Integer userId) {
    this.userId = userId;
  }
  
  public LocalDateTime getExpiration() {
    return expiration;
  }
  
  public void setExpiration(LocalDateTime expiration) {
    this.expiration = expiration;
  }
  
  public String getToken() {
    return token;
  }
  
  public void setToken(String token) {
    this.token = token;
  }
  
  public String getTokenFile() {
    return tokenFile;
  }
  
  public void setTokenFile(String tokenFile) {
    this.tokenFile = tokenFile;
  }
  
  public CidAndPort getPidAndPort() {
    return pidAndPort;
  }
  
  public boolean maybeRenew(LocalDateTime now) {
    return now.isAfter(expiration) || now.isEqual(expiration);
  }
  
  public boolean isExpired() {
    LocalDateTime now = DateUtils.getNow();
    return now.isAfter(expiration) || now.isEqual(expiration);
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JupyterJWTDTO that = (JupyterJWTDTO) o;
    return Objects.equals(projectId, that.projectId) && Objects.equals(userId, that.userId);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(projectId, userId);
  }
}
