/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.models.dto;

import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public class ModelsEndpointDTO {
  private Project parentProject;
  private Long datasetInodeId;
  
  private Integer parentProjectId;
  private String parentProjectName;
  
  public ModelsEndpointDTO() {
  }
  
  public ModelsEndpointDTO(Project parentProj, Long datasetInodeId) {
    this.parentProject = parentProj;
    this.parentProjectId = parentProj.getId();
    this.parentProjectName = parentProj.getName();
    this.datasetInodeId = datasetInodeId;
  }
  
  public static ModelsEndpointDTO fromDataset(Dataset ds) {
    return new ModelsEndpointDTO(ds.getProject(), ds.getInodeId());
  }
  
  @XmlElement(name = "id")
  public Integer getParentProjectId() {
    return parentProjectId;
  }
  
  public void setParentProjectId(Integer parentProjectId) {
    this.parentProjectId = parentProjectId;
  }
  
  @XmlElement(name = "name")
  public String getParentProjectName() {
    return parentProjectName;
  }
  
  public void setParentProjectName(String parentProjectName) {
    this.parentProjectName = parentProjectName;
  }
  
  @XmlTransient
  public Project getParentProject() {
    return parentProject;
  }
  
  public void setParentProject(Project parentProject) {
    this.parentProject = parentProject;
  }
  
  @XmlTransient
  public Long getDatasetInodeId() {
    return datasetInodeId;
  }
  
  public void setDatasetInodeId(Long datasetInodeId) {
    this.datasetInodeId = datasetInodeId;
  }
}