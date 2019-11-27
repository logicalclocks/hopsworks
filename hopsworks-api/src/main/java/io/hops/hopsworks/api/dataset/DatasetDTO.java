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
package io.hops.hopsworks.api.dataset;

import io.hops.hopsworks.api.dataset.inode.attribute.InodeAttributeDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.dataset.DatasetPermissions;
import io.hops.hopsworks.common.dao.dataset.DatasetType;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class DatasetDTO extends RestDTO<DatasetDTO> {
  
  private Integer id;
  private String name; //display name (will be with :: if the ds is shared)
  private String description;
  private String publicId;
  private Integer publicDataset;
  private Boolean searchable;
  private Boolean accepted; //share status
  private Boolean shared;
  private Integer sharedWith;
  private List<ProjectDTO> projectsSharedWith;
  private DatasetPermissions permission;
  private DatasetType datasetType;
  private InodeAttributeDTO attributes;
  
  public DatasetDTO() {
  }
  
  public InodeAttributeDTO getAttributes() {
    return attributes;
  }
  
  public void setAttributes(InodeAttributeDTO attributes) {
    this.attributes = attributes;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getPublicId() {
    return publicId;
  }
  
  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }
  
  public Integer getPublicDataset() {
    return publicDataset;
  }
  
  public void setPublicDataset(Integer publicDataset) {
    this.publicDataset = publicDataset;
  }
  
  public Boolean isSearchable() {
    return searchable;
  }
  
  public void setSearchable(Boolean searchable) {
    this.searchable = searchable;
  }
  
  public Boolean getAccepted() {
    return accepted;
  }
  
  public void setAccepted(Boolean accepted) {
    this.accepted = accepted;
  }
  
  public Integer getSharedWith() {
    return sharedWith;
  }
  
  public void setSharedWith(Integer sharedWith) {
    this.sharedWith = sharedWith;
  }
  
  public List<ProjectDTO> getProjectsSharedWith() {
    return projectsSharedWith;
  }
  
  public void setProjectsSharedWith(List<ProjectDTO> projectsSharedWith) {
    this.projectsSharedWith = projectsSharedWith;
  }
  
  public Boolean isShared() {
    return shared;
  }
  
  public void setShared(Boolean shared) {
    this.shared = shared;
  }
  
  public DatasetPermissions getPermission() {
    return permission;
  }
  
  public void setPermission(DatasetPermissions permission) {
    this.permission = permission;
  }
  
  public DatasetType getDatasetType() {
    return datasetType;
  }
  
  public void setDatasetType(DatasetType datasetType) {
    this.datasetType = datasetType;
  }
  
  @Override
  public String toString() {
    return "DatasetDTO{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", publicId='" + publicId + '\'' +
      ", publicDataset=" + publicDataset +
      ", searchable=" + searchable +
      ", accepted=" + accepted +
      ", shared=" + shared +
      ", sharedWith=" + sharedWith +
      ", projectsSharedWith=" + projectsSharedWith +
      ", permission=" + permission +
      ", datasetType=" + datasetType +
      ", attributes=" + attributes +
      '}';
  }
}