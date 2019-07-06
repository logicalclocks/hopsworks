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

package io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs;

import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information of a HopsFS connection for a feature store, can be converted to JSON or
 * XML representation using jaxb.
 */
@XmlRootElement
public class FeaturestoreHopsfsConnectorDTO extends FeaturestoreStorageConnectorDTO {

  private String hopsfsPath;
  private String datasetName;

  public FeaturestoreHopsfsConnectorDTO() {
    super(null, null, null, null, null);
  }
  
  public FeaturestoreHopsfsConnectorDTO(FeaturestoreHopsfsConnector featurestoreHopsfsConnector) {
    super(featurestoreHopsfsConnector.getId(), featurestoreHopsfsConnector.getDescription(),
        featurestoreHopsfsConnector.getName(), featurestoreHopsfsConnector.getFeaturestore().getId(),
        FeaturestoreStorageConnectorType.HopsFS);
    this.hopsfsPath = null;
    this.datasetName = featurestoreHopsfsConnector.getHopsfsDataset().getName();
  }
  
  @XmlElement
  public String getHopsfsPath() {
    return hopsfsPath;
  }
  
  public void setHopsfsPath(String hopsfsPath) {
    this.hopsfsPath = hopsfsPath;
  }

  @XmlElement
  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  @Override
  public String toString() {
    return "FeaturestoreHopsfsConnectorDTO{" +
        "hopsfsPath='" + hopsfsPath + '\'' +
        ", datasetName='" + datasetName + '\'' +
        '}';
  }
}
