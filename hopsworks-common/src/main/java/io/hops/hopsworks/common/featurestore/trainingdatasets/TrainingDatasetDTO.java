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

package io.hops.hopsworks.common.featurestore.trainingdatasets;

import io.hops.hopsworks.common.featurestore.trainingdatasets.split.TrainingDatasetSplitDTO;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetType;
import io.hops.hopsworks.common.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO containing the human-readable information of a trainingDataset, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
public class TrainingDatasetDTO extends FeaturestoreEntityDTO {
  
  private String dataFormat;
  private TrainingDatasetType trainingDatasetType;
  // set defaults so old clients don't get broken
  private List<TrainingDatasetSplitDTO> splits = new ArrayList<>();
  private Long seed = null;

  private Integer storageConnectorId; // Need to know which storage connector to use it
  // ID + type would be unique. However momentarily keep also the name, until we switch to
  // rest api v2 with expansion on the storage controller.
  private String storageConnectorName;
  private FeaturestoreStorageConnectorType storageConnectorType;

  // This is here for the frontend. The frontend uses a rest call to get the total size of
  // a subdirectory - the rest call requires the inode id.
  private Long inodeId;

  public TrainingDatasetDTO() {
  }

  public TrainingDatasetDTO(TrainingDataset trainingDataset) {
    super(trainingDataset.getFeaturestore().getId(),
        trainingDataset.getName(),
        trainingDataset.getCreated(),
        trainingDataset.getCreator(), trainingDataset.getVersion(),
        (List) trainingDataset.getJobs(), trainingDataset.getId());
    setDescription(trainingDataset.getDescription());
    setFeatures(trainingDataset.getFeatures().stream().map(tdf -> new FeatureDTO(tdf.getName(),
      tdf.getType(), tdf.getDescription(), tdf.getPrimary(), false, null)).collect(Collectors.toList()));
    this.dataFormat = trainingDataset.getDataFormat();
    this.trainingDatasetType = trainingDataset.getTrainingDatasetType();
    this.splits =
      trainingDataset.getSplits().stream().map(tds -> new TrainingDatasetSplitDTO(tds.getName(), tds.getPercentage()))
        .collect(Collectors.toList());
    this.seed = trainingDataset.getSeed();
  }
  
  @XmlElement
  public String getDataFormat() {
    return dataFormat;
  }
  
  public void setDataFormat(String dataFormat) {
    this.dataFormat = dataFormat;
  }

  public Integer getStorageConnectorId() {
    return storageConnectorId;
  }

  public void setStorageConnectorId(Integer storageConnectorId) {
    this.storageConnectorId = storageConnectorId;
  }

  public String getStorageConnectorName() {
    return storageConnectorName;
  }

  public void setStorageConnectorName(String storageConnectorName) {
    this.storageConnectorName = storageConnectorName;
  }

  public FeaturestoreStorageConnectorType getStorageConnectorType() {
    return storageConnectorType;
  }

  public void setStorageConnectorType(FeaturestoreStorageConnectorType storageConnectorType) {
    this.storageConnectorType = storageConnectorType;
  }

  public Long getInodeId() {
    return inodeId;
  }

  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }

  @XmlElement
  public TrainingDatasetType getTrainingDatasetType() {
    return trainingDatasetType;
  }
  
  public void setTrainingDatasetType(
    TrainingDatasetType trainingDatasetType) {
    this.trainingDatasetType = trainingDatasetType;
  }

  @XmlElement
  public List<TrainingDatasetSplitDTO> getSplits() {
    return splits;
  }
  
  public void setSplits(
    List<TrainingDatasetSplitDTO> splits) {
    this.splits = splits;
  }
  
  @XmlElement
  public Long getSeed() {
    return seed;
  }
  
  public void setSeed(Long seed) {
    this.seed = seed;
  }
  
  @Override
  public String toString() {
    return "TrainingDatasetDTO{" +
      "dataFormat='" + dataFormat + '\'' +
      ", trainingDatasetType=" + trainingDatasetType +
      ", splits=" + splits +
      ", seed=" + seed +
      ", storageConnectorId=" + storageConnectorId +
      ", storageConnectorName='" + storageConnectorName + '\'' +
      ", storageConnectorType=" + storageConnectorType +
      ", inodeId=" + inodeId +
      '}';
  }
}
