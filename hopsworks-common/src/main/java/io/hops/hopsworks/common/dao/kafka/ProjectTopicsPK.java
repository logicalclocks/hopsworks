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

package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class ProjectTopicsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "topic_name")
  private String topicName;
  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;

  public ProjectTopicsPK() {
  }

  public ProjectTopicsPK(String topicName, int projectId) {
    this.topicName = topicName;
    this.projectId = projectId;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (topicName != null ? topicName.hashCode() : 0);
    hash += (int) projectId;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ProjectTopicsPK)) {
      return false;
    }
    ProjectTopicsPK other = (ProjectTopicsPK) object;
    if ((this.topicName == null && other.topicName != null) || (this.topicName
            != null && !this.topicName.equals(other.topicName))) {
      return false;
    }
    if (this.projectId != other.projectId) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.kafka.ProjectTopicsPK[ topicName=" + topicName
            + ", projectId=" + projectId + " ]";
  }

}
