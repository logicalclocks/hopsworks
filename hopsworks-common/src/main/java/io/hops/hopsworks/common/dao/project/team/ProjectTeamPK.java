/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.project.team;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Embeddable
public class ProjectTeamPK implements Serializable {

  @Basic(optional = false)
  @Column(name = "project_id")
  private Integer projectId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "team_member")
  private String teamMember;

  public ProjectTeamPK() {
  }

  public ProjectTeamPK(Integer projectId, String teamMember) {
    this.projectId = projectId;
    this.teamMember = teamMember;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getTeamMember() {
    return teamMember;
  }

  public void setTeamMember(String teamMember) {
    this.teamMember = teamMember;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectId != null ? projectId.hashCode() : 0);
    hash += (teamMember != null ? teamMember.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectTeamPK)) {
      return false;
    }
    ProjectTeamPK other = (ProjectTeamPK) object;
    if ((this.projectId == null && other.projectId != null) || (this.projectId
            != null
            && !this.projectId.equals(other.projectId))) {
      return false;
    }
    if ((this.teamMember == null && other.teamMember != null)
            || (this.teamMember != null && !this.teamMember.equals(
                    other.teamMember))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.ProjectTeamPK[ projectId=" + projectId
            + ", teamMember="
            + teamMember + " ]";
  }

}
