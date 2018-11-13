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
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.serving.tf.TfServingStatusEnum;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.serving.tf.TfServingWrapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@XmlRootElement
@ApiModel(value = "Represents a TfServing model")
public class TfServingView implements Serializable {

  private static final long serialVersionUID = 1L;

  private Integer id;
  private String modelName;
  private String modelPath;
  private Integer modelVersion;
  private Integer availableInstances;
  private Integer requestedInstances;
  private Integer nodePort;
  private Date created;
  private Boolean batchingEnabled;

  // TODO(Fabio): use expansions here
  private String creator;

  @XmlElement
  private TfServingStatusEnum status;

  // TODO(Fabio): use expansions here
  private TopicDTO kafkaTopicDTO;

  public TfServingView() { }

  public TfServingView(TfServingWrapper tfServingWrapper) {
    this.id = tfServingWrapper.getTfServing().getId();
    this.modelName = tfServingWrapper.getTfServing().getModelName();
    this.modelPath = tfServingWrapper.getTfServing().getModelPath();
    this.modelVersion = tfServingWrapper.getTfServing().getVersion();
    this.availableInstances = tfServingWrapper.getAvailableReplicas();
    this.requestedInstances = tfServingWrapper.getTfServing().getInstances();
    this.nodePort = tfServingWrapper.getNodePort();
    this.created = tfServingWrapper.getTfServing().getCreated();
    this.status = tfServingWrapper.getStatus();
    this.kafkaTopicDTO = tfServingWrapper.getKafkaTopicDTO();
    this.batchingEnabled = tfServingWrapper.getTfServing().isBatchingEnabled();

    Users user = tfServingWrapper.getTfServing().getCreator();
    this.creator = user.getFname() + " " + user.getLname();
  }

  @ApiModelProperty(value = "ID of the TfServing entry" )
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @ApiModelProperty(value = "Name of the model to serve")
  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  @ApiModelProperty(value = "HopsFS directory path containing the model")
  public String getModelPath() {
    return modelPath;
  }

  public void setModelPath(String modelPath) {
    this.modelPath = modelPath;
  }

  @ApiModelProperty(value = "Model version to serve")
  public Integer getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(Integer modelVersion) {
    this.modelVersion = modelVersion;
  }

  @ApiModelProperty(value = "Number of TfServing instances to use for serving")
  public Integer getRequestedInstances() {
    return requestedInstances;
  }

  public void setRequestedInstances(Integer requestedInstances) {
    this.requestedInstances = requestedInstances;
  }

  @ApiModelProperty(value = "Number of TfServing instances available for serving", readOnly = true)
  public Integer getAvailableInstances() {
    return availableInstances;
  }

  public void setAvailableInstances(Integer availableInstances) {
    this.availableInstances = availableInstances;
  }

  @ApiModelProperty(value = "Port on which the TfServing instance(s) are listening", readOnly = true)
  public Integer getNodePort() {
    return nodePort;
  }

  public void setNodePort(Integer nodePort) {
    this.nodePort = nodePort;
  }

  @ApiModelProperty(value = "Date on which the TfServing entry was created", readOnly = true)
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @ApiModelProperty(value = "User whom created the TfServing entry", readOnly = true)
  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  @ApiModelProperty(value = "Status of the TfServing entry", readOnly = true)
  public TfServingStatusEnum getStatus() {
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

  @JsonIgnore
  public TfServingWrapper getTfServingWrapper() {

    TfServingWrapper tfServingWrapper = new TfServingWrapper(
        new TfServing(id, modelName, modelPath, modelVersion, requestedInstances, batchingEnabled));
    tfServingWrapper.setKafkaTopicDTO(kafkaTopicDTO);

    return tfServingWrapper;
  }
}
