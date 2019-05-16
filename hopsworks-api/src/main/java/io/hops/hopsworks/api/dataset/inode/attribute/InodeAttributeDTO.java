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
package io.hops.hopsworks.api.dataset.inode.attribute;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class InodeAttributeDTO {
  
  private String name;
  private String path;
  private Long size;
  private String permission;
  private Date modificationTime;
  private Date accessTime;
  private Boolean underConstruction;
  private String owner;
  private String group;
  private Boolean dir;
  private Long id;
  private Long parentId;
  
  public InodeAttributeDTO() {
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public Long getSize() {
    return size;
  }
  
  public void setSize(Long size) {
    this.size = size;
  }
  
  public String getPermission() {
    return permission;
  }
  
  public void setPermission(String permission) {
    this.permission = permission;
  }
  
  public Date getModificationTime() {
    return modificationTime;
  }
  
  public void setModificationTime(Date modificationTime) {
    this.modificationTime = modificationTime;
  }
  
  public Date getAccessTime() {
    return accessTime;
  }
  
  public void setAccessTime(Date accessTime) {
    this.accessTime = accessTime;
  }
  
  public Boolean isUnderConstruction() {
    return underConstruction;
  }
  
  public void setUnderConstruction(Boolean underConstruction) {
    this.underConstruction = underConstruction;
  }
  
  public String getOwner() {
    return owner;
  }
  
  public void setOwner(String owner) {
    this.owner = owner;
  }
  
  public String getGroup() {
    return group;
  }
  
  public void setGroup(String group) {
    this.group = group;
  }
  
  public Boolean isDir() {
    return dir;
  }
  
  public void setDir(Boolean dir) {
    this.dir = dir;
  }
  
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }
  
  public Long getParentId() {
    return parentId;
  }
  
  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }
  
  @Override
  public String toString() {
    return "InodeAttributeDTO{" +
      "name='" + name + '\'' +
      ", path='" + path + '\'' +
      ", size=" + size +
      ", permission='" + permission + '\'' +
      ", modificationTime=" + modificationTime +
      ", accessTime=" + accessTime +
      ", underConstruction=" + underConstruction +
      ", owner='" + owner + '\'' +
      ", group='" + group + '\'' +
      ", isDir=" + dir +
      ", id=" + id +
      ", parentId=" + parentId +
      '}';
  }
}