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

package io.hops.hopsworks.persistence.entity.airflow;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@Embeddable
public class MaterializedJWTID implements Serializable {
  private static final long serialVersionUID = 1L;
  
  // Order is important. Always append!
  public enum USAGE {
    AIRFLOW,
    JUPYTER
  }
  
  @Column(name = "project_id",
          nullable = false)
  private Integer projectId;
  
  @Column(name = "user_id",
          nullable = false)
  private Integer userId;
  
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "\"usage\"",
          nullable = false)
  private USAGE usage;
  
  public MaterializedJWTID() {
  }
  
  public MaterializedJWTID(Integer projectId, Integer userId, USAGE usage) {
    this.projectId = projectId;
    this.userId = userId;
    this.usage = usage;
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
  
  public USAGE getUsage() {
    return usage;
  }
  
  public void setUsage(USAGE usage) {
    this.usage = usage;
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + userId;
    result = 31 * result + projectId;
    result = 31 * result + usage.ordinal();
    return result;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof MaterializedJWTID) {
      MaterializedJWTID id = (MaterializedJWTID) o;
      return this.projectId.equals(id.projectId) && this.userId.equals(id.userId) && this.usage.equals(id.usage);
    }
    return false;
  }
  
  @Override
  public String toString() {
    return "PID: " + projectId + " - UID: " + userId + " - USAGE: " + usage;
  }
}
