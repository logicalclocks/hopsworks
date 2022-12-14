/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance.explicit.dto;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.provenance.ProvExplicitNode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ProvNodeDTO<A extends RestDTO> {
  private A artifact;
  
  @XmlElement(name = "artifact_type")
  private ProvExplicitNode.Type artifactType;
  private boolean deleted;
  private boolean shared;
  private boolean accessible;
  private boolean traversed;
  @XmlElement(name = "exception_cause")
  private String exceptionCause;
 
  public ProvNodeDTO() {
  }
  
  public A getArtifact() {
    return artifact;
  }
  
  public void setArtifact(A artifact) {
    this.artifact = artifact;
  }
  
  public ProvExplicitNode.Type getArtifactType() {
    return artifactType;
  }
  
  public void setArtifactType(ProvExplicitNode.Type artifactType) {
    this.artifactType = artifactType;
  }
  
  public boolean isDeleted() {
    return deleted;
  }
  
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
  
  public boolean isShared() {
    return shared;
  }
  
  public void setShared(boolean shared) {
    this.shared = shared;
  }
  
  public boolean isAccessible() {
    return accessible;
  }
  
  public void setAccessible(boolean accessible) {
    this.accessible = accessible;
  }
  
  public boolean isTraversed() {
    return traversed;
  }
  
  public void setTraversed(boolean traversed) {
    this.traversed = traversed;
  }
  
  public String getExceptionCause() {
    return exceptionCause;
  }
  
  public void setExceptionCause(String exceptionCause) {
    this.exceptionCause = exceptionCause;
  }
}
