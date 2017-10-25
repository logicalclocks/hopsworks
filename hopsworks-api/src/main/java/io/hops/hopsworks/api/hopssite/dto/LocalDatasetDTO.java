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
