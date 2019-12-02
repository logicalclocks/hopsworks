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
package io.hops.hopsworks.common.dao.kafka.schemas;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@Embeddable
public class SubjectsPK implements Serializable {
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 255)
  @Column(name = "\"subject\"")
  private String subject;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "version")
  private Integer version;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private Integer projectId;
  
  public SubjectsPK() {
  }
  
  public SubjectsPK(String subject, Integer version, Integer projectId) {
    this.subject = subject;
    this.version = version;
    this.projectId = projectId;
  }
  
  public String getSubject() {
    return subject;
  }
  
  public void setSubject(String subject) {
    this.subject = subject;
  }
  
  public int getVersion() {
    return version;
  }
  
  public void setVersion(int version) {
    this.version = version;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    SubjectsPK that = (SubjectsPK) o;
    
    if (version != that.version) {
      return false;
    }
    if (subject != null ? !subject.equals(that.subject) : that.subject != null) {
      return false;
    }
    return projectId != null ? projectId.equals(that.projectId) : that.projectId == null;
  }
  
  @Override
  public int hashCode() {
    int result = subject != null ? subject.hashCode() : 0;
    result = 31 * result + version;
    result = 31 * result + (projectId != null ? projectId.hashCode() : 0);
    return result;
  }
  
  @Override
  public String toString() {
    return "SubjectsPK{" +
      "subject='" + subject + '\'' +
      ", version=" + version +
      ", projectId=" + projectId +
      '}';
  }
}
