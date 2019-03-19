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

package io.hops.hopsworks.common.dao.airflow;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class AirflowMaterialID implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Column(name = "project_id",
          nullable = false)
  private Integer projectId;
  
  @Column(name = "user_id",
          nullable = false)
  private Integer userId;
  
  public AirflowMaterialID() {
  }
  
  public AirflowMaterialID(Integer projectId, Integer userId) {
    this.projectId = projectId;
    this.userId = userId;
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
  
  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + userId;
    result = 31 * result + projectId;
    return result;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof AirflowMaterialID) {
      AirflowMaterialID id = (AirflowMaterialID) o;
      return this.projectId.equals(id.projectId) && this.userId.equals(id.userId);
    }
    return false;
  }
  
  @Override
  public String toString() {
    return "PID: " + projectId + " - UID: " + userId;
  }
}
