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

import io.hops.hopsworks.common.api.RestDTO;

import java.io.Serializable;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TopicDTO extends RestDTO<TopicDTO> implements Serializable {

  private String name;

  private Integer numOfReplicas;

  private Integer numOfPartitions;

  private String schemaName;

  private Integer schemaVersion;
  
  private String schemaContent;
  
  private Integer ownerProjectId;
  
  private Boolean isShared;
  
  private Boolean accepted;

  public TopicDTO() {
  }
  
  public TopicDTO(UriInfo uriInfo) {
    this.setHref(uriInfo.getAbsolutePathBuilder().build());
  }

  public TopicDTO(String name) {
    this.name = name;
  }
  
  public TopicDTO(String name, Integer ownerProjectId, String schemaName, Integer schemaVersion, String
    schemaContent, Boolean isShared, Boolean accepted) {
    this.name = name;
    this.ownerProjectId = ownerProjectId;
    this.schemaName = schemaName;
    this.schemaVersion = schemaVersion;
    this.schemaContent = schemaContent;
    this.isShared = isShared;
    this.accepted = accepted;
  }

  public TopicDTO(String name, Integer numOfReplicas, Integer numOfPartitions) {
    this.name = name;
    this.numOfReplicas = numOfReplicas;
    this.numOfPartitions = numOfPartitions;
  }

  public TopicDTO(String name, String schemaName, Integer schemaVersion, Boolean isShared) {
    this.name = name;
    this.schemaName = schemaName;
    this.schemaVersion = schemaVersion;
    this.isShared = isShared;
  }

  public TopicDTO(String name, Integer numOfReplicas, Integer numOfPartitions,
          String schemaName, Integer schemaVersion) {
    this.name = name;
    this.numOfReplicas = numOfReplicas;
    this.numOfPartitions = numOfPartitions;
    this.schemaName = schemaName;
    this.schemaVersion = schemaVersion;
  }
  
  public TopicDTO(String name, Integer numOfReplicas, Integer numOfPartitions,
    String schemaName, Integer schemaVersion, Integer ownerProjectId, Boolean isShared) {
    this.name = name;
    this.numOfReplicas = numOfReplicas;
    this.numOfPartitions = numOfPartitions;
    this.schemaName = schemaName;
    this.schemaVersion = schemaVersion;
    this.isShared = isShared;
    this.ownerProjectId = ownerProjectId;
  }
  
  public TopicDTO(String topicName, String subject, Integer subjectVersion, String schema) {
    this.name = topicName;
    this.schemaName = subject;
    this.schemaVersion = subjectVersion;
    this.schemaContent = schema;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getNumOfPartitions() {
    return numOfPartitions;
  }

  public Integer getNumOfReplicas() {
    return numOfReplicas;
  }

  public void setNumOfPartitions(Integer numOfPartitions) {
    this.numOfPartitions = numOfPartitions;
  }

  public void setNumOfReplicas(Integer numOfReplicas) {
    this.numOfReplicas = numOfReplicas;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public Integer getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public void setSchemaVersion(Integer schemaVersion) {
    this.schemaVersion = schemaVersion;
  }
  
  public Boolean isShared() {
    return isShared;
  }
  
  public void setShared(Boolean shared) {
    isShared = shared;
  }
  
  public Integer getOwnerProjectId() {
    return ownerProjectId;
  }
  
  public void setOwnerProjectId(Integer ownerProjectId) {
    this.ownerProjectId = ownerProjectId;
  }
  
  public String getSchemaContent() {
    return schemaContent;
  }
  
  public void setSchemaContent(String schemaContent) {
    this.schemaContent = schemaContent;
  }
  
  public Boolean getShared() {
    return isShared;
  }
  
  public Boolean getAccepted() {
    return accepted;
  }
  
  public void setAccepted(Boolean accepted) {
    this.accepted = accepted;
  }
}
