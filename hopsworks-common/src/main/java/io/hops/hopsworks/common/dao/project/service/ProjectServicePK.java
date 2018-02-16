/*
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
 *
 */

package io.hops.hopsworks.common.dao.project.service;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

@Embeddable
public class ProjectServicePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "service")
  private ProjectServiceEnum service;

  public ProjectServicePK() {
  }

  public ProjectServicePK(int projectId, ProjectServiceEnum service) {
    this.projectId = projectId;
    this.service = service;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public ProjectServiceEnum getService() {
    return service;
  }

  public void setService(ProjectServiceEnum service) {
    this.service = service;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) projectId;
    hash += (service != null ? service.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectServicePK)) {
      return false;
    }
    ProjectServicePK other = (ProjectServicePK) object;
    if (this.projectId != other.projectId) {
      return false;
    }
    if ((this.service == null && other.service != null) || (this.service != null
            && !this.service.equals(other.service))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "[" + projectId + "," + service + "]";
  }

}
