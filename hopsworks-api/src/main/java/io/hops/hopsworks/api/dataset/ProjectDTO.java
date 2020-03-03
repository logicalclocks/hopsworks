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

import io.hops.hopsworks.common.dao.project.Project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ProjectDTO {
  private Integer id;
  private String name;
  private String owner;
  
  public ProjectDTO() {
  }
  
  public ProjectDTO(Project project) {
    this.id = project.getId();
    this.name = project.getName();
    this.owner = project.getOwner().getFname() + " " + project.getOwner().getLname();
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
  
  public String getOwner() {
    return owner;
  }
  
  public void setOwner(String owner) {
    this.owner = owner;
  }
  
  @Override
  public String toString() {
    return "ProjectDTO{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", owner='" + owner + '\'' +
      '}';
  }
}
