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

package io.hops.hopsworks.common.dao.project.service;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.project.Project;

@Entity
@Table(name = "hopsworks.project_services")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectServices.findAll",
          query
          = "SELECT s FROM ProjectServices s"),
  @NamedQuery(name = "ProjectServices.findByProject",
          query
          = "SELECT s FROM ProjectServices s WHERE s.project = :project"),
  @NamedQuery(name = "ProjectServices.findServicesByProject",
          query
          = "SELECT s.projectServicePK.service FROM ProjectServices s "
          + "WHERE s.project = :project ORDER BY s.projectServicePK.service"),
  @NamedQuery(name = "ProjectServices.findByService",
          query
          = "SELECT s FROM ProjectServices s WHERE s.projectServicePK.service = :service"),
  @NamedQuery(name = "ProjectServices.isServiceEnabledForProject",
    query
        = "SELECT s FROM ProjectServices s WHERE s.project = :project and " +
        "s.projectServicePK.service = :service")})

public class ProjectServices implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected ProjectServicePK projectServicePK;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Project project;

  public ProjectServices() {
  }

  public ProjectServices(ProjectServicePK projectServicesPK) {
    this.projectServicePK = projectServicesPK;
  }

  public ProjectServices(Project project, ProjectServiceEnum service) {
    this.projectServicePK = new ProjectServicePK(project.getId(), service);
  }

  public ProjectServicePK getProjectServicesPK() {
    return projectServicePK;
  }

  public void setProjectServicesPK(ProjectServicePK projectServicesPK) {
    this.projectServicePK = projectServicesPK;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectServicePK != null ? projectServicePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectServices)) {
      return false;
    }
    ProjectServices other = (ProjectServices) object;
    if ((this.projectServicePK == null && other.projectServicePK != null)
            || (this.projectServicePK != null && !this.projectServicePK.equals(
                    other.projectServicePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "[" + project + "," + projectServicePK.getService() + " ]";
  }

}
