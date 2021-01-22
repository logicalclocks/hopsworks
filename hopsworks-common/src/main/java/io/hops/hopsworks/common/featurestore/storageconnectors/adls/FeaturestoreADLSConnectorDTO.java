/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.adls;

import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class FeaturestoreADLSConnectorDTO extends FeaturestoreStorageConnectorDTO {
  private Integer generation;

  private String directoryId;
  private String applicationId;
  private String serviceCredential;

  private String accountName;
  private String containerName;

  private List<OptionDTO> sparkOptions;

  public FeaturestoreADLSConnectorDTO() {
  }

  public FeaturestoreADLSConnectorDTO(FeaturestoreConnector featurestoreConnector) {
    super(featurestoreConnector);
    this.generation = featurestoreConnector.getAdlsConnector().getGeneration();

    this.directoryId = featurestoreConnector.getAdlsConnector().getDirectoryId();
    this.applicationId = featurestoreConnector.getAdlsConnector().getApplicationId();

    this.accountName = featurestoreConnector.getAdlsConnector().getAccountName();
    this.containerName = featurestoreConnector.getAdlsConnector().getContainerName();
  }

  public Integer getGeneration() {
    return generation;
  }

  public void setGeneration(Integer generation) {
    this.generation = generation;
  }

  public String getDirectoryId() {
    return directoryId;
  }

  public void setDirectoryId(String directoryId) {
    this.directoryId = directoryId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getServiceCredential() {
    return serviceCredential;
  }

  public void setServiceCredential(String serviceCredential) {
    this.serviceCredential = serviceCredential;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getContainerName() {
    return containerName;
  }

  public void setContainerName(String containerName) {
    this.containerName = containerName;
  }

  public List<OptionDTO> getSparkOptions() {
    return sparkOptions;
  }

  public void setSparkOptions(List<OptionDTO> sparkOptions) {
    this.sparkOptions = sparkOptions;
  }

  @Override
  public String toString() {
    return "FeaturestoreADLConnectorDTO{" +
        "directoryId='" + directoryId + '\'' +
        ", applicationId='" + applicationId + '\'' +
        ", accountName='" + accountName + '\'' +
        ", containerName='" + containerName + '\'' +
        ", serviceCredential='" + serviceCredential + '\'' +
        '}';
  }
}
