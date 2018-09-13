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

package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "shared_topics",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "SharedTopics.findAll",
          query = "SELECT s FROM SharedTopics s"),
  @NamedQuery(name = "SharedTopics.findByTopicName",
          query
          = "SELECT s FROM SharedTopics s WHERE s.sharedTopicsPK.topicName = :topicName"),
  @NamedQuery(name = "SharedTopics.findByProjectId",
          query
          = "SELECT s FROM SharedTopics s WHERE s.sharedTopicsPK.projectId = :projectId"),
  @NamedQuery(name = "SharedTopics.findByTopicNameAndProjectId",
          query
          = "SELECT s FROM SharedTopics s WHERE s.sharedTopicsPK.projectId "
          + "= :projectId and s.sharedTopicsPK.topicName = :topicName")})
public class SharedTopics implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected SharedTopicsPK sharedTopicsPK;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 1000)
  @Column(name = "owner_id")
  private Integer projectId;

  public SharedTopics() {
  }

  public SharedTopics(SharedTopicsPK sharedTopicsPK, Integer projectId) {
    this.sharedTopicsPK = sharedTopicsPK;
    this.projectId = projectId;
  }

  public SharedTopics(String topicName, int owningId, Integer projectId) {
    this.sharedTopicsPK = new SharedTopicsPK(topicName, projectId);
    this.projectId = owningId;
  }

  public SharedTopicsPK getSharedTopicsPK() {
    return sharedTopicsPK;
  }

  public void setSharedTopicsPK(SharedTopicsPK sharedTopicsPK) {
    this.sharedTopicsPK = sharedTopicsPK;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += projectId;
    hash += (sharedTopicsPK != null ? sharedTopicsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof SharedTopics)) {
      return false;
    }
    SharedTopics other = (SharedTopics) object;
    if (this.projectId != other.projectId) {
      return false;
    }
    if ((this.sharedTopicsPK == null && other.sharedTopicsPK != null)
            || (this.sharedTopicsPK != null
            && !this.sharedTopicsPK.equals(other.sharedTopicsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.kafka.SharedTopics[ sharedTopicsPK="
            + sharedTopicsPK + " ] shared to: " + this.projectId;
  }

}
