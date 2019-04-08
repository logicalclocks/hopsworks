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

package io.hops.hopsworks.common.dao.featurestore.app;

import io.hops.hopsworks.common.dao.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDatasetDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * DTO containing the metadata of a featurestore.
 * Can be converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"featurestore", "featuregroups", "trainingDatasets"})
public class FeaturestoreMetadataDTO {
  
  private FeaturestoreDTO featurestore;
  private List<FeaturegroupDTO> featuregroups;
  private List<TrainingDatasetDTO> trainingDatasets;

  public FeaturestoreMetadataDTO() {
  }
  
  public FeaturestoreMetadataDTO(FeaturestoreDTO featurestore,
    List<FeaturegroupDTO> featuregroups,
    List<TrainingDatasetDTO> trainingDatasets) {
    this.featurestore = featurestore;
    this.featuregroups = featuregroups;
    this.trainingDatasets = trainingDatasets;
  }
  
  @XmlElement
  public List<FeaturegroupDTO> getFeaturegroups() {
    return featuregroups;
  }

  @XmlElement
  public List<TrainingDatasetDTO> getTrainingDatasets() {
    return trainingDatasets;
  }
  
  @XmlElement
  public FeaturestoreDTO getFeaturestore() {
    return featurestore;
  }
  
  public void setFeaturegroups(List<FeaturegroupDTO> featuregroups) {
    this.featuregroups = featuregroups;
  }

  public void setTrainingDatasets(List<TrainingDatasetDTO> trainingDatasets) {
    this.trainingDatasets = trainingDatasets;
  }
  
  public void setFeaturestore(FeaturestoreDTO featurestore) {
    this.featurestore = featurestore;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreMetadataDTO{" +
      "featurestore=" + featurestore +
      ", featuregroups=" + featuregroups +
      ", trainingDatasets=" + trainingDatasets +
      '}';
  }
}
