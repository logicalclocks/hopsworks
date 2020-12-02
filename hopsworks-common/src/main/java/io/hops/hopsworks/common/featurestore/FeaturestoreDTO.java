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

package io.hops.hopsworks.common.featurestore;

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * DTO containing the human-readable information of a featurestore, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
public class FeaturestoreDTO {
  
  private Integer featurestoreId;
  private String featurestoreName;
  private Date created;
  private String hdfsStorePath;
  private String projectName;
  private Integer projectId;
  private String featurestoreDescription;
  private Long inodeId;
  private String onlineFeaturestoreName;
  private Double onlineFeaturestoreSize;
  private String offlineFeaturestoreName;
  private String hiveEndpoint;
  private String mysqlServerEndpoint;
  private Boolean onlineEnabled = false;
  private Long numFeatureGroups;
  private Long numTrainingDatasets;
  private Long numStorageConnectors;
  
  public FeaturestoreDTO() {
  }
  
  public FeaturestoreDTO(Featurestore featurestore) {
    this.featurestoreId = featurestore.getId();
    this.created = featurestore.getCreated();
    this.projectName = featurestore.getProject().getName();
    this.projectId = featurestore.getProject().getId();
    this.hdfsStorePath = null;
    this.featurestoreDescription = null;
    this.featurestoreName = null;
    this.inodeId = null;
    this.onlineFeaturestoreName = null;
    this.offlineFeaturestoreName = null;
    this.hiveEndpoint = null;
    this.mysqlServerEndpoint = null;
    this.onlineFeaturestoreSize = 0.0;
    this.onlineEnabled = false;
  }
  
  @XmlElement
  public String getHdfsStorePath() {
    return hdfsStorePath;
  }
  
  public void setHdfsStorePath(String hdfsStorePath) {
    this.hdfsStorePath = hdfsStorePath;
  }
  
  @XmlElement
  public String getFeaturestoreName() {
    return featurestoreName;
  }
  
  public void setFeaturestoreName(String featurestoreName) {
    this.featurestoreName = featurestoreName;
  }
  
  @XmlElement
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }
  
  @XmlElement
  public Date getCreated() {
    return created;
  }

  @XmlElement
  public String getProjectName() {
    return projectName;
  }

  @XmlElement
  public Integer getProjectId() {
    return projectId;
  }
  
  @XmlElement(nillable = true)
  public String getFeaturestoreDescription() {
    return featurestoreDescription;
  }
  
  public void setFeaturestoreDescription(String featurestoreDescription) {
    this.featurestoreDescription = featurestoreDescription;
  }
  
  @XmlElement
  public Long getInodeId() {
    return inodeId;
  }
  
  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }
  
  @XmlElement
  public String getOnlineFeaturestoreName() {
    return onlineFeaturestoreName;
  }
  
  @XmlElement
  public String getOfflineFeaturestoreName() {
    return offlineFeaturestoreName;
  }
  
  public void setOfflineFeaturestoreName(String offlineFeaturestoreName) {
    this.offlineFeaturestoreName = offlineFeaturestoreName;
  }
  
  @XmlElement
  public Double getOnlineFeaturestoreSize() {
    return onlineFeaturestoreSize;
  }
  
  public void setOnlineFeaturestoreSize(Double onlineFeaturestoreSize) {
    this.onlineFeaturestoreSize = onlineFeaturestoreSize;
  }
  
  @XmlElement
  public String getHiveEndpoint() {
    return hiveEndpoint;
  }
  
  public void setHiveEndpoint(String hiveEndpoint) {
    this.hiveEndpoint = hiveEndpoint;
  }
  
  @XmlElement
  public String getMysqlServerEndpoint() {
    return mysqlServerEndpoint;
  }
  
  public void setMysqlServerEndpoint(String mysqlServerEndpoint) {
    this.mysqlServerEndpoint = mysqlServerEndpoint;
  }

  public void setOnlineFeaturestoreName(String onlineFeaturestoreName) {
    this.onlineFeaturestoreName = onlineFeaturestoreName;
  }

  @XmlElement
  public Boolean getOnlineEnabled() {
    return onlineEnabled;
  }
  
  public void setOnlineEnabled(Boolean onlineEnabled) {
    this.onlineEnabled = onlineEnabled;
  }

  public Long getNumFeatureGroups() {
    return numFeatureGroups;
  }

  public void setNumFeatureGroups(Long numFeatureGroups) {
    this.numFeatureGroups = numFeatureGroups;
  }

  public Long getNumTrainingDatasets() {
    return numTrainingDatasets;
  }

  public void setNumTrainingDatasets(Long numTrainingDatasets) {
    this.numTrainingDatasets = numTrainingDatasets;
  }

  public Long getNumStorageConnectors() {
    return numStorageConnectors;
  }

  public void setNumStorageConnectors(Long numStorageConnectors) {
    this.numStorageConnectors = numStorageConnectors;
  }

  @Override
  public String toString() {
    return "FeaturestoreDTO{" +
        "featurestoreId=" + featurestoreId +
        ", featurestoreName='" + featurestoreName + '\'' +
        ", created=" + created +
        ", hdfsStorePath='" + hdfsStorePath + '\'' +
        ", projectName='" + projectName + '\'' +
        ", projectId=" + projectId +
        ", featurestoreDescription='" + featurestoreDescription + '\'' +
        ", inodeId=" + inodeId +
        ", onlineFeaturestoreName='" + onlineFeaturestoreName + '\'' +
        ", onlineFeaturestoreSize=" + onlineFeaturestoreSize +
        ", offlineFeaturestoreName='" + offlineFeaturestoreName + '\'' +
        ", hiveEndpoint='" + hiveEndpoint + '\'' +
        ", mysqlServerEndpoint='" + mysqlServerEndpoint + '\'' +
        ", onlineEnabled=" + onlineEnabled +
        ", numFeatureGroups=" + numFeatureGroups +
        ", numTrainingDatasets=" + numTrainingDatasets +
        ", numStorageConnectors=" + numStorageConnectors +
        '}';
  }
}
