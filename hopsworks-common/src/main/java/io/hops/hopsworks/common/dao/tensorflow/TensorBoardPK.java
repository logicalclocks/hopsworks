/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.tensorflow;


import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

import java.io.Serializable;

@Embeddable
public class TensorBoardPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "user_id")
  private int userId;

  public TensorBoardPK() {
  }

  public TensorBoardPK(int projectId, int userId) {
    this.projectId = projectId;
    this.userId = userId;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  @Override
  public int hashCode() {
    return (getProjectId() + "_" + getUserId()).hashCode();
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof TensorBoardPK)) {
      return false;
    }
    TensorBoardPK other = (TensorBoardPK) object;
    if (this.getProjectId() != other.getProjectId()
        || this.getUserId() != other.getUserId()) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.TensorBoardPK[ project=" +
            getProjectId() + ", user=" + getUserId() + " ]";
  }
}
