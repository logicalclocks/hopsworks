/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.project.team;

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "hopsworks.project_team")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectTeam.findRoleForUserInProject",
          query
          = "SELECT s FROM ProjectTeam s WHERE s.project = :project AND s.user = :user"),
  @NamedQuery(name = "ProjectTeam.findAll",
          query = "SELECT s FROM ProjectTeam s"),
  @NamedQuery(name = "ProjectTeam.findByProject",
          query = "SELECT s FROM ProjectTeam s WHERE s.project = :project"),
  @NamedQuery(name = "ProjectTeam.findActiveByTeamMember",
          query
          = "SELECT s FROM ProjectTeam s WHERE s.user = :user AND s.teamRole != \"Under removal\" "),
  @NamedQuery(name = "ProjectTeam.findByTeamRole",
          query = "SELECT s FROM ProjectTeam s WHERE s.teamRole = :teamRole"),
  @NamedQuery(name = "ProjectTeam.countStudiesByMember",
          query
          = "SELECT COUNT(s) FROM ProjectTeam s WHERE s.user = :user"),
  @NamedQuery(name = "ProjectTeam.countMembersForProjectAndRole",
          query
          = "SELECT COUNT(DISTINCT s.projectTeamPK.teamMember) FROM ProjectTeam s "
          + "WHERE s.project=:project AND s.teamRole = :teamRole"),
  @NamedQuery(name = "ProjectTeam.countAllMembersForProject",
          query
          = "SELECT COUNT(DISTINCT s.projectTeamPK.teamMember) FROM ProjectTeam s"
          + " WHERE s.project = :project"),
  @NamedQuery(name = "ProjectTeam.findMembersByRoleInProject",
          query
          = "SELECT s FROM ProjectTeam s WHERE s.project = :project "
          + "AND s.teamRole = :teamRole"),
  @NamedQuery(name = "ProjectTeam.findAllMemberStudiesForUser",
          query
          = "SELECT st.project from ProjectTeam st WHERE st.user = :user"),
  @NamedQuery(name = "ProjectTeam.findAllJoinedStudiesForUser",
          query
          = "SELECT st.project from ProjectTeam st WHERE st.user = :user "
          + "AND NOT st.project.owner = :user")})
public class ProjectTeam implements Serializable {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Project project;

  @JoinColumn(name = "team_member",
          referencedColumnName = "email",
          insertable
          = false,
          updatable = false)
  @ManyToOne(optional = false,
          fetch = FetchType.EAGER)//needed to delete users from hdfs in HDFSUserController.deleteProjectUsers()
  private Users user;

  @EmbeddedId
  protected ProjectTeamPK projectTeamPK;

  @Column(name = "team_role")
  private String teamRole;

  @Basic(optional = false)
  @NotNull
  @Column(name = "added")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  public ProjectTeam() {
  }

  public ProjectTeam(ProjectTeamPK projectTeamPK) {
    this.projectTeamPK = projectTeamPK;
  }

  public ProjectTeam(ProjectTeamPK projectTeamPK, Date timestamp) {
    this.projectTeamPK = projectTeamPK;
    this.timestamp = timestamp;
  }

  public ProjectTeam(Project project, Users user) {
    this.projectTeamPK = new ProjectTeamPK(project.getId(), user.getEmail());
  }

  public ProjectTeamPK getProjectTeamPK() {
    return projectTeamPK;
  }

  public void setProjectTeamPK(ProjectTeamPK projectTeamPK) {
    this.projectTeamPK = projectTeamPK;
  }

  public String getTeamRole() {
    return teamRole;
  }

  public void setTeamRole(String teamRole) {
    this.teamRole = teamRole;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectTeamPK != null ? projectTeamPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectTeam)) {
      return false;
    }
    ProjectTeam other = (ProjectTeam) object;
    if ((this.projectTeamPK == null && other.projectTeamPK != null)
            || (this.projectTeamPK != null && !this.projectTeamPK.equals(
                    other.projectTeamPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.ProjectTeam[ projectTeamPK=" + projectTeamPK
            + " ]";
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

}
