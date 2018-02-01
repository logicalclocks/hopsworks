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

package io.hops.hopsworks.api.hopssite.dto;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LocalDatasetDTO {
  private int inodeId;
  private String name;
  private String description;
  private String projectName;
  private Date createDate;
  private Date publishedOn;
  private long size;

  public LocalDatasetDTO() {
  }

  public LocalDatasetDTO(int InodeId, String projectName) {
    this.inodeId = InodeId;
    this.projectName = projectName;
  }

  public LocalDatasetDTO(int inodeId, String name, String description, String projectName) {
    this.inodeId = inodeId;
    this.name = name;
    this.description = description;
    this.projectName = projectName;
  }

  public LocalDatasetDTO(int inodeId, String name, String description, String projectName, Date createDate,
          Date publishedOn, long size) {
    this.inodeId = inodeId;
    this.name = name;
    this.description = description;
    this.projectName = projectName;
    this.createDate = createDate;
    this.publishedOn = publishedOn;
    this.size = size;
  }

  public int getInodeId() {
    return inodeId;
  }

  public void setInodeId(int InodeId) {
    this.inodeId = InodeId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
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

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Date getPublishedOn() {
    return publishedOn;
  }

  public void setPublishedOn(Date publishedOn) {
    this.publishedOn = publishedOn;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }
  
}
