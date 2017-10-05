package io.hops.hopsworks.api.hopssite.dto;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LocalDatasetDTO {
  private int InodeId;
  private String name;
  private String description;
  private String projectName;
  private Date createDate;
  private Date publishedOn;
  private long size;

  public LocalDatasetDTO() {
  }

  public LocalDatasetDTO(int InodeId, String projectName) {
    this.InodeId = InodeId;
    this.projectName = projectName;
  }

  public LocalDatasetDTO(int InodeId, String name, String description, String projectName) {
    this.InodeId = InodeId;
    this.name = name;
    this.description = description;
    this.projectName = projectName;
  }

  public LocalDatasetDTO(int InodeId, String name, String description, String projectName, Date createDate,
          Date publishedOn, long size) {
    this.InodeId = InodeId;
    this.name = name;
    this.description = description;
    this.projectName = projectName;
    this.createDate = createDate;
    this.publishedOn = publishedOn;
    this.size = size;
  }

  public int getInodeId() {
    return InodeId;
  }

  public void setInodeId(int InodeId) {
    this.InodeId = InodeId;
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
