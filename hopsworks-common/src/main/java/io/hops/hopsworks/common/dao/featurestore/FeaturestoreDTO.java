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

package io.hops.hopsworks.common.dao.featurestore;

import io.hops.hopsworks.common.dao.featurestore.storage_connectors.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.jdbc.FeaturestoreJdbcConnectorDTO;
import io.hops.hopsworks.common.dao.featurestore.storage_connectors.s3.FeaturestoreS3ConnectorDTO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;

/**
 * DTO containing the human-readable information of a featurestore, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"featurestoreId", "featurestoreName", "featurestoreDescription",
    "created", "hdfsStorePath", "projectName", "projectId", "inodeId", "featurestoreJdbcConnections",
    "featurestoreS3Connections", "featurestoreHopsfsConnections"})
public class FeaturestoreDTO {

  private Integer featurestoreId;
  private String featurestoreName;
  private Date created;
  private String hdfsStorePath;
  private String projectName;
  private Integer projectId;
  private String featurestoreDescription;
  private Long inodeId;
  private List<FeaturestoreJdbcConnectorDTO> featurestoreJdbcConnections;
  private List<FeaturestoreS3ConnectorDTO> featurestoreS3Connections;
  private List<FeaturestoreHopsfsConnectorDTO> featurestoreHopsfsConnections;

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
  }

  @XmlElement
  public String getHdfsStorePath() {
    return hdfsStorePath;
  }

  @XmlElement
  public String getFeaturestoreName() {
    return featurestoreName;
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

  @XmlElement
  public Long getInodeId() {
    return inodeId;
  }
  
  @XmlElement
  public List<FeaturestoreJdbcConnectorDTO> getFeaturestoreJdbcConnections() {
    return featurestoreJdbcConnections;
  }
  
  @XmlElement
  public List<FeaturestoreS3ConnectorDTO> getFeaturestoreS3Connections() {
    return featurestoreS3Connections;
  }
  
  @XmlElement
  public List<FeaturestoreHopsfsConnectorDTO> getFeaturestoreHopsfsConnections() {
    return featurestoreHopsfsConnections;
  }
  
  public void setFeaturestoreDescription(String featurestoreDescription) {
    this.featurestoreDescription = featurestoreDescription;
  }

  public void setFeaturestoreName(String featurestoreName) {
    this.featurestoreName = featurestoreName;
  }

  public void setHdfsStorePath(String hdfsStorePath) {
    this.hdfsStorePath = hdfsStorePath;
  }

  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }
  
  public void setFeaturestoreJdbcConnections(
    List<FeaturestoreJdbcConnectorDTO> featurestoreJdbcConnections) {
    this.featurestoreJdbcConnections = featurestoreJdbcConnections;
  }
  
  public void setFeaturestoreS3Connections(
    List<FeaturestoreS3ConnectorDTO> featurestoreS3Connections) {
    this.featurestoreS3Connections = featurestoreS3Connections;
  }
  
  public void setFeaturestoreHopsfsConnections(
    List<FeaturestoreHopsfsConnectorDTO> featurestoreHopsfsConnections) {
    this.featurestoreHopsfsConnections = featurestoreHopsfsConnections;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreDTO{" +
        "featurestoreId=" + featurestoreId +
        ", featurestoreName='" + featurestoreName + '\'' +
        ", created='" + created + '\'' +
        ", hdfsStorePath='" + hdfsStorePath + '\'' +
        ", projectName='" + projectName + '\'' +
        ", projectId=" + projectId +
        ", featurestoreDescription='" + featurestoreDescription + '\'' +
        ", inodeId=" + inodeId +
        '}';
  }
}
