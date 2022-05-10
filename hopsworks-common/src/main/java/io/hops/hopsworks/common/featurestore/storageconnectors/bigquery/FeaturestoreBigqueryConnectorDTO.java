/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.bigquery;

import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class FeaturestoreBigqueryConnectorDTO extends FeaturestoreStorageConnectorDTO {
  
  private String keyPath;
  private String parentProject;
  private String dataset;
  private String queryTable;
  private String queryProject;
  private String materializationDataset;
  private List<OptionDTO> arguments;
  
  public FeaturestoreBigqueryConnectorDTO() {
  }
  
  public FeaturestoreBigqueryConnectorDTO(FeaturestoreConnector featurestoreConnector) {
    super(featurestoreConnector);
  }
  
  public List<OptionDTO> getArguments() {
    return arguments;
  }
  
  public void setArguments(List<OptionDTO> arguments) {
    this.arguments = arguments;
  }
  
  public String getKeyPath() {
    return keyPath;
  }
  
  public String getParentProject() {
    return parentProject;
  }
  
  public String getDataset() {
    return dataset;
  }
  
  public String getQueryTable() {
    return queryTable;
  }
  
  public String getQueryProject() {
    return queryProject;
  }
  
  public String getMaterializationDataset() {
    return materializationDataset;
  }
  
  public void setQueryProject(String queryProject) {
    this.queryProject = queryProject;
  }
  
  public void setMaterializationDataset(String materializationDataset) {
    this.materializationDataset = materializationDataset;
  }
  
  public void setDataset(String dataset) {
    this.dataset = dataset;
  }
  
  public void setQueryTable(String queryTable) {
    this.queryTable = queryTable;
  }
  
  public void setKeyPath(String keyPath) {
    this.keyPath = keyPath;
  }
  
  public void setParentProject(String parentProject) {
    this.parentProject = parentProject;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreBigqueryConnectorDTO{" +
      "keyPath='" + keyPath + '\'' +
      ", parentProject='" + parentProject + '\'' +
      ", dataset='" + dataset + '\'' +
      ", queryTable='" + queryTable + '\'' +
      ", queryProject='" + queryProject + '\'' +
      ", materializationDataset='" + materializationDataset + '\'' +
      '}';
  }
}
