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

import io.hops.hopsworks.common.dao.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO containing the human-readable information of a trainingDataset, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
public class TrainingDatasetDTO extends FeaturestoreEntityDTO {

  private String hdfsStorePath;
  private String dataFormat;
  private Long size;

  public TrainingDatasetDTO() {
  }

  public TrainingDatasetDTO(TrainingDataset trainingDataset) {
    super(trainingDataset.getFeaturestore().getId(),
        trainingDataset.getCreated(),
        trainingDataset.getCreator(), trainingDataset.getVersion(),
        (List) trainingDataset.getStatistics(), trainingDataset.getJob(),
        trainingDataset.getId());
    setDescription(trainingDataset.getDescription());
    setName(trainingDataset.getName());
    setInodeId(trainingDataset.getInode().getId());
    setFeatures(trainingDataset.getFeatures().stream().map(tdf -> new FeatureDTO(tdf.getName(),
        tdf.getType(), tdf.getDescription(), new Boolean(tdf.getPrimary() == 1), false)).collect(Collectors.toList()));
    this.size = trainingDataset.getInode().getSize();
    this.dataFormat = trainingDataset.getDataFormat();
    this.hdfsStorePath = null;
  }

  @XmlElement
  public String getHdfsStorePath() {
    return hdfsStorePath;
  }

  @XmlElement
  public String getDataFormat() {
    return dataFormat;
  }

  @XmlElement
  public Long getSize() {
    return size;
  }

  public void setHdfsStorePath(String hdfsStorePath) {
    this.hdfsStorePath = hdfsStorePath;
  }

  @Override
  public String toString() {
    return "TrainingDatasetDTO{" +
        ", hdfsStorePath='" + hdfsStorePath + '\'' +
        ", dataFormat='" + dataFormat + '\'' +
        '}';
  }
}
