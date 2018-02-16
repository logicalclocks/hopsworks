/*
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
 *
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
