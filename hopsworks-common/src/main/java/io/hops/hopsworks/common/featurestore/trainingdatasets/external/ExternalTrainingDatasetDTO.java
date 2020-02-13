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

package io.hops.hopsworks.common.featurestore.trainingdatasets.external;

import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information of a external training dataset in the feature store, can be
 * converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
public class ExternalTrainingDatasetDTO extends TrainingDatasetDTO {
  
  
  private Integer s3ConnectorId;
  private String s3ConnectorName;
  private String s3ConnectorBucket;
  
  public ExternalTrainingDatasetDTO() {
    super();
  }
  
  public ExternalTrainingDatasetDTO(TrainingDataset trainingDataset) {
    super(trainingDataset);
    this.s3ConnectorId = trainingDataset.getExternalTrainingDataset().getFeaturestoreS3Connector().getId();
    this.s3ConnectorName = trainingDataset.getExternalTrainingDataset().getFeaturestoreS3Connector().getName();
    this.s3ConnectorBucket = trainingDataset.getExternalTrainingDataset().getFeaturestoreS3Connector().getBucket();
  }
  
  @XmlElement
  public Integer getS3ConnectorId() {
    return s3ConnectorId;
  }
  
  public void setS3ConnectorId(Integer s3ConnectorId) {
    this.s3ConnectorId = s3ConnectorId;
  }
  
  @XmlElement
  public String getS3ConnectorName() {
    return s3ConnectorName;
  }
  
  public void setS3ConnectorName(String s3ConnectorName) {
    this.s3ConnectorName = s3ConnectorName;
  }
  
  @XmlElement
  public String getS3ConnectorBucket() {
    return s3ConnectorBucket;
  }
  
  public void setS3ConnectorBucket(String s3ConnectorBucket) {
    this.s3ConnectorBucket = s3ConnectorBucket;
  }
  
  @Override
  public String toString() {
    return "ExternalTrainingDatasetDTO{" +
      "s3ConnectorId=" + s3ConnectorId +
      ", s3ConnectorName='" + s3ConnectorName + '\'' +
      ", s3ConnectorBucket='" + s3ConnectorBucket + '\'' +
      '}';
  }
}
