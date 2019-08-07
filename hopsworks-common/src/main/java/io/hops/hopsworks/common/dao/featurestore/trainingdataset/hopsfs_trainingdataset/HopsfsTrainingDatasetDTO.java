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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset.hopsfs_trainingdataset;

import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information of a training dataset in the feature store stored in Hopsfs, can be
 * converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
public class HopsfsTrainingDatasetDTO extends TrainingDatasetDTO {
  
  private String hdfsStorePath;
  private Long size;
  private Integer hopsfsConnectorId;
  private String hopsfsConnectorName;
  private Long inodeId;
  
  
  public HopsfsTrainingDatasetDTO() {
    super();
  }
  
  public HopsfsTrainingDatasetDTO(TrainingDataset trainingDataset) {
    super(trainingDataset);
    setInodeId(trainingDataset.getHopsfsTrainingDataset().getInode().getId());
    this.size = trainingDataset.getHopsfsTrainingDataset().getInode().getSize();
    this.hopsfsConnectorId = trainingDataset.getHopsfsTrainingDataset().getFeaturestoreHopsfsConnector().getId();
    this.hopsfsConnectorName = trainingDataset.getHopsfsTrainingDataset().getFeaturestoreHopsfsConnector().getName();
  }
  
  @XmlElement
  public String getHdfsStorePath() {
    return hdfsStorePath;
  }
  
  public void setHdfsStorePath(String hdfsStorePath) {
    this.hdfsStorePath = hdfsStorePath;
  }
  
  @XmlElement
  public Long getSize() {
    return size;
  }
  
  public void setSize(Long size) {
    this.size = size;
  }
  
  @XmlElement
  public Integer getHopsfsConnectorId() {
    return hopsfsConnectorId;
  }
  
  public void setHopsfsConnectorId(Integer hopsfsConnectorId) {
    this.hopsfsConnectorId = hopsfsConnectorId;
  }
  
  @XmlElement
  public String getHopsfsConnectorName() {
    return hopsfsConnectorName;
  }
  
  public void setHopsfsConnectorName(String hopsfsConnectorName) {
    this.hopsfsConnectorName = hopsfsConnectorName;
  }
  
  @XmlElement
  public Long getInodeId() {
    return inodeId;
  }
  
  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }
  
  @Override
  public String toString() {
    return "HopsfsTrainingDatasetDTO{" +
      "hdfsStorePath='" + hdfsStorePath + '\'' +
      ", size=" + size +
      ", hopsfsConnectorId=" + hopsfsConnectorId +
      ", hopsfsConnectorName='" + hopsfsConnectorName + '\'' +
      ", inodeId=" + inodeId +
      '}';
  }
}
