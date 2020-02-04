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


package io.hops.hopsworks.api.serving;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.serving.ServingType;
import io.hops.hopsworks.common.dao.serving.Serving;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@XmlRootElement
@ApiModel(value = "Represents a Serving model")
public class ServingView implements Serializable {

  private static final long serialVersionUID = 1L;

  private Integer id;
  private String name;
  private String artifactPath;
  private Integer modelVersion;
  private Integer availableInstances;
  private Integer requestedInstances;
  private Integer nodePort;
  private Date created;
  private Boolean batchingEnabled;
  private ServingType servingType;

  // TODO(Fabio): use expansions here
  private String creator;

  @XmlElement
  private ServingStatusEnum status;

  // TODO(Fabio): use expansions here
  private TopicDTO kafkaTopicDTO;

  public ServingView() { }

  public ServingView(ServingWrapper servingWrapper) {
    this.id = servingWrapper.getServing().getId();
    this.name = servingWrapper.getServing().getName();
    this.artifactPath = servingWrapper.getServing().getArtifactPath();
    this.modelVersion = servingWrapper.getServing().getVersion();
    this.availableInstances = servingWrapper.getAvailableReplicas();
    this.requestedInstances = servingWrapper.getServing().getInstances();
    this.nodePort = servingWrapper.getNodePort();
    this.created = servingWrapper.getServing().getCreated();
    this.status = servingWrapper.getStatus();
    this.kafkaTopicDTO = servingWrapper.getKafkaTopicDTO();
    this.batchingEnabled = servingWrapper.getServing().isBatchingEnabled();
    this.servingType = servingWrapper.getServing().getServingType();
    Users user = servingWrapper.getServing().getCreator();
    this.creator = user.getFname() + " " + user.getLname();
  }

  @ApiModelProperty(value = "ID of the Serving entry" )
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @ApiModelProperty(value = "Name of the serving")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @ApiModelProperty(value = "HOPSFS directory path containing the model (tf) or python script (sklearn)")
  public String getArtifactPath() {
    return artifactPath;
  }

  public void setArtifactPath(String artifactPath) {
    this.artifactPath = artifactPath;
  }

  @ApiModelProperty(value = "Version of the serving")
  public Integer getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(Integer modelVersion) {
    this.modelVersion = modelVersion;
  }

  @ApiModelProperty(value = "Number of Serving instances to use for serving")
  public Integer getRequestedInstances() {
    return requestedInstances;
  }

  public void setRequestedInstances(Integer requestedInstances) {
    this.requestedInstances = requestedInstances;
  }

  @ApiModelProperty(value = "Number of Serving instances available for serving", readOnly = true)
  public Integer getAvailableInstances() {
    return availableInstances;
  }

  public void setAvailableInstances(Integer availableInstances) {
    this.availableInstances = availableInstances;
  }

  @ApiModelProperty(value = "Port on which the Serving instance(s) are listening", readOnly = true)
  public Integer getNodePort() {
    return nodePort;
  }

  public void setNodePort(Integer nodePort) {
    this.nodePort = nodePort;
  }

  @ApiModelProperty(value = "Date on which the Serving entry was created", readOnly = true)
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @ApiModelProperty(value = "User whom created the Serving entry", readOnly = true)
  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  @ApiModelProperty(value = "ServiceStatus of the Serving entry", readOnly = true)
  public ServingStatusEnum getStatus() {
    return status;
  }

  @ApiModelProperty(value = "Is request batching enabled")
  public Boolean isBatchingEnabled() {
    return batchingEnabled;
  }

  public void setBatchingEnabled(Boolean batchingEnabled) {
    this.batchingEnabled = batchingEnabled;
  }

  public TopicDTO getKafkaTopicDTO() {
    return kafkaTopicDTO;
  }

  public void setKafkaTopicDTO(TopicDTO kafkaTopicDTO) {
    this.kafkaTopicDTO = kafkaTopicDTO;
  }
  
  @ApiModelProperty(value = "Type of serving, sklearn or tfserving")
  public ServingType getServingType() {
    return servingType;
  }
  
  public void setServingType(ServingType servingType) {
    this.servingType = servingType;
  }
  
  @JsonIgnore
  public ServingWrapper getServingWrapper() {

    ServingWrapper servingWrapper = new ServingWrapper(
        new Serving(id, name, artifactPath, modelVersion, requestedInstances, batchingEnabled,
            servingType));
    servingWrapper.setKafkaTopicDTO(kafkaTopicDTO);

    return servingWrapper;
  }
}
