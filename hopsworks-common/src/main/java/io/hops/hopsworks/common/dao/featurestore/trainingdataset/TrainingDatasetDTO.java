/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.external_trainingdataset.ExternalTrainingDatasetDTO;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.hopsfs_trainingdataset.HopsfsTrainingDatasetDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO containing the human-readable information of a trainingDataset, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
@XmlSeeAlso({HopsfsTrainingDatasetDTO.class, ExternalTrainingDatasetDTO.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = HopsfsTrainingDatasetDTO.class, name = "HopsfsTrainingDatasetDTO"),
  @JsonSubTypes.Type(value = ExternalTrainingDatasetDTO.class, name = "ExternalTrainingDatasetDTO")})
public class TrainingDatasetDTO extends FeaturestoreEntityDTO {
  
  private String dataFormat;
  private TrainingDatasetType trainingDatasetType;

  public TrainingDatasetDTO() {
  }

  public TrainingDatasetDTO(TrainingDataset trainingDataset) {
    super(trainingDataset.getFeaturestore().getId(),
        trainingDataset.getCreated(),
        trainingDataset.getCreator(), trainingDataset.getVersion(),
        (List) trainingDataset.getStatistics(), trainingDataset.getJob(),
        trainingDataset.getId());
    setDescription(trainingDataset.getDescription());
    setFeatures(trainingDataset.getFeatures().stream().map(tdf -> new FeatureDTO(tdf.getName(),
        tdf.getType(), tdf.getDescription(), tdf.getPrimary() == 1, false)).collect(Collectors.toList()));
    this.dataFormat = trainingDataset.getDataFormat();
    this.trainingDatasetType = trainingDataset.getTrainingDatasetType();
  }
  
  @XmlElement
  public String getDataFormat() {
    return dataFormat;
  }
  
  public void setDataFormat(String dataFormat) {
    this.dataFormat = dataFormat;
  }
  
  @XmlElement
  public TrainingDatasetType getTrainingDatasetType() {
    return trainingDatasetType;
  }
  
  public void setTrainingDatasetType(
    TrainingDatasetType trainingDatasetType) {
    this.trainingDatasetType = trainingDatasetType;
  }
  
  @Override
  public String toString() {
    return "TrainingDatasetDTO{" +
      "dataFormat='" + dataFormat + '\'' +
      ", trainingDatasetType=" + trainingDatasetType +
      '}';
  }
}
