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
 *
 */
package io.hops.hopsworks.common.dao.rstudio;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class RStudioSettingsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 150)
  @Column(name = "team_member")
  private String email;

  public RStudioSettingsPK() {
  }

  public RStudioSettingsPK(int projectId, String email) {
    this.projectId = projectId;
    this.email = email;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String teamMember) {
    this.email = teamMember;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (email != null ? email.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof RStudioSettingsPK)) {
      return false;
    }
    RStudioSettingsPK other = (RStudioSettingsPK) object;
    if (this.projectId != other.projectId) {
      return false;
    }
    if ((this.email == null && other.email != null) ||
            (this.email != null && !this.email.equals(other.email))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.JupyterSettingsPK[ projectId=" +
            projectId + ", teamMember=" + email + " ]";
  }



}
