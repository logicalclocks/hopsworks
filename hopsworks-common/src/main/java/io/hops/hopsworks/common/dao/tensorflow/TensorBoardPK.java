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


import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;


import java.io.Serializable;

@Embeddable
public class TensorBoardPK implements Serializable {

  @JoinColumn(name = "project_id",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  @JoinColumn(name = "user_id",
      referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users user;

  public TensorBoardPK() {
  }

  public TensorBoardPK(Project projectId, Users user) {
    this.project = projectId;
    this.user = user;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) getProject().getId();
    String email = getUser().getEmail();
    hash += (email != null ? email.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof TensorBoardPK)) {
      return false;
    }
    TensorBoardPK other = (TensorBoardPK) object;
    if (this.getProject().getId() != other.getProject().getId()
        || this.getUser().getUid() != other.getUser().getUid()) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.TensorBoardPK[ project=" +
            getProject() + ", user=" + getUser() + " ]";
  }
}
