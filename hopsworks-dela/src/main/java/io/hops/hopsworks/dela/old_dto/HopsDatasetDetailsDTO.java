package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HopsDatasetDetailsDTO {
  private String datasetName;
  private Integer projectId;
  private Integer datasetId;

  public HopsDatasetDetailsDTO() {
  }

  public HopsDatasetDetailsDTO(String datasetName, Integer projectId, Integer datasetId) {
    this.datasetName = datasetName;
    this.projectId = projectId;
    this.datasetId = datasetId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }
  
}
