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


package io.hops.hopsworks.api.serving;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.serving.ServingStatusCondition;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.persistence.entity.serving.DeployableComponentResources;
import io.hops.hopsworks.persistence.entity.serving.BatchingConfiguration;
import io.hops.hopsworks.persistence.entity.serving.InferenceLogging;
import io.hops.hopsworks.persistence.entity.serving.ModelFramework;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.serving.ServingTool;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

@ApiModel(value = "Represents a Serving model")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServingView implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private Integer id;
  private String name;
  private String description;
  private String modelPath;
  private String modelName;
  private Integer modelVersion;
  private ModelFramework modelFramework;
  private Integer artifactVersion;
  private String predictor;
  private String transformer;
  private Integer availableInstances;
  private Integer availableTransformerInstances;
  private Integer requestedInstances;
  private Integer requestedTransformerInstances;
  private Integer internalPort; // community only
  private String hopsworksInferencePath;
  private String modelServerInferencePath;
  private Date created;
  private InferenceLogging inferenceLogging;
  private ModelServer modelServer;
  private ServingTool servingTool;
  private DeployableComponentResources predictorResources;
  private DeployableComponentResources transformerResources;
  private Date deployed;
  private String revision;
  private ServingStatusCondition condition;
  private BatchingConfiguration batchingConfiguration;

  // TODO(Fabio): use expansions here
  private String creator;

  private ServingStatusEnum status;

  // TODO(Fabio): use expansions here
  private TopicDTO kafkaTopicDTO;

  public ServingView() { }

  public ServingView(ServingWrapper servingWrapper) {
    this.id = servingWrapper.getServing().getId();
    this.name = servingWrapper.getServing().getName();
    this.description = servingWrapper.getServing().getDescription();
    this.modelPath = servingWrapper.getServing().getModelPath();
    this.predictor = servingWrapper.getServing().getPredictor();
    this.transformer = servingWrapper.getServing().getTransformer();
    this.modelName = servingWrapper.getServing().getModelName();
    this.modelVersion = servingWrapper.getServing().getModelVersion();
    this.modelFramework = servingWrapper.getServing().getModelFramework();
    this.artifactVersion = servingWrapper.getServing().getArtifactVersion();
    this.availableInstances = servingWrapper.getAvailableReplicas();
    this.availableTransformerInstances = servingWrapper.getAvailableTransformerReplicas();
    this.requestedInstances = servingWrapper.getServing().getInstances();
    this.requestedTransformerInstances = servingWrapper.getServing().getTransformerInstances();
    this.internalPort = servingWrapper.getInternalPort(); // community
    this.hopsworksInferencePath = servingWrapper.getHopsworksInferencePath();
    this.modelServerInferencePath = servingWrapper.getModelServerInferencePath();
    this.created = servingWrapper.getServing().getCreated();
    this.status = servingWrapper.getStatus();
    this.kafkaTopicDTO = servingWrapper.getKafkaTopicDTO();
    this.inferenceLogging = servingWrapper.getServing().getInferenceLogging();
    this.modelServer = servingWrapper.getServing().getModelServer();
    this.servingTool = servingWrapper.getServing().getServingTool();
    this.deployed = servingWrapper.getServing().getDeployed();
    this.revision = servingWrapper.getServing().getRevision();
    this.condition = servingWrapper.getCondition();
    Users user = servingWrapper.getServing().getCreator();
    this.creator = user.getFname() + " " + user.getLname();
    this.predictorResources = servingWrapper.getServing().getPredictorResources();
    this.transformerResources = servingWrapper.getServing().getTransformerResources();
    this.batchingConfiguration = servingWrapper.getServing().getBatchingConfiguration();
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
  
  @ApiModelProperty(value = "Description of the serving")
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  
  @ApiModelProperty(value = "HOPSFS directory path containing the model")
  public String getModelPath() { return modelPath; }
  public void setModelPath(String modelPath) { this.modelPath = modelPath; }
  
  @ApiModelProperty(value = "Predictor script name")
  public String getPredictor() {
    return predictor;
  }
  public void setPredictor(String predictor) {
    this.predictor = predictor;
  }
  
  @ApiModelProperty(value = "Transformer script name")
  public String getTransformer() {
    return transformer;
  }
  public void setTransformer(String transformer) {
    this.transformer = transformer;
  }

  @ApiModelProperty(value = "Name of the model")
  public String getModelName() {
    return modelName;
  }
  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  @ApiModelProperty(value = "Version of the model")
  public Integer getModelVersion() {
    return modelVersion;
  }
  public void setModelVersion(Integer modelVersion) {
    this.modelVersion = modelVersion;
  }

  @ApiModelProperty(value = "Framework of the model")
  public ModelFramework getModelFramework() { return modelFramework; }
  public void setModelFramework(ModelFramework modelFramework) { this.modelFramework = modelFramework;}
  
  @ApiModelProperty(value = "Version of the artifact")
  public Integer getArtifactVersion() {
    return artifactVersion;
  }
  public void setArtifactVersion(Integer artifactVersion) {
    this.artifactVersion = artifactVersion;
  }

  @ApiModelProperty(value = "Number of serving instances to use for serving")
  public Integer getRequestedInstances() {
    return requestedInstances;
  }
  public void setRequestedInstances(Integer requestedInstances) {
    this.requestedInstances = requestedInstances;
  }

  @ApiModelProperty(value = "Number of serving instances to use for the transformer")
  public Integer getRequestedTransformerInstances() {
    return requestedTransformerInstances;
  }
  public void setRequestedTransformerInstances(Integer transformerInstances) {
    this.requestedTransformerInstances = transformerInstances;
  }

  @ApiModelProperty(value = "Number of serving instances available for serving", readOnly = true)
  public Integer getAvailableInstances() {
    return availableInstances;
  }
  public void setAvailableInstances(Integer availableInstances) {
    this.availableInstances = availableInstances;
  }

  @ApiModelProperty(value = "Number of serving instances available for transformers serving", readOnly = true)
  public Integer getAvailableTransformerInstances() {
    return availableTransformerInstances;
  }
  public void setAvailableTransformerInstances(Integer availableInstances) {
    this.availableTransformerInstances = availableInstances;
  }
  
  @ApiModelProperty(value = "Internal port on which the Serving instance(s) are listening", readOnly = true)
  public Integer getInternalPort() { return internalPort; }
  public void setInternalPort(Integer internalPort) { this.internalPort = internalPort; }
  
  @ApiModelProperty(value = "Hopsworks REST API inference path on which the deployment can be reached", readOnly = true)
  public String getHopsworksInferencePath() { return hopsworksInferencePath; }
  public void setHopsworksInferencePath(String hopsworksInferencePath) {
    this.hopsworksInferencePath = hopsworksInferencePath;
  }
  
  @ApiModelProperty(value = "Model server inference path on which the deployment can be reached", readOnly = true)
  public String getModelServerInferencePath() { return modelServerInferencePath; }
  public void setModelServerInferencePath(String modelServerInferencePath) {
    this.modelServerInferencePath = modelServerInferencePath;
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

  public TopicDTO getKafkaTopicDTO() {
    return kafkaTopicDTO;
  }
  public void setKafkaTopicDTO(TopicDTO kafkaTopicDTO) {
    this.kafkaTopicDTO = kafkaTopicDTO;
  }
  
  public InferenceLogging getInferenceLogging() { return inferenceLogging; }
  public void setInferenceLogging(InferenceLogging inferenceLogging) { this.inferenceLogging = inferenceLogging; }
  
  @ApiModelProperty(value = "Model server, Tensorflow Serving or Python")
  public ModelServer getModelServer() {
    return modelServer;
  }
  public void setModelServer(ModelServer modelServer) {
    this.modelServer = modelServer;
  }
  
  @ApiModelProperty(value = "Serving tool, default or kserve")
  public ServingTool getServingTool() {
    return servingTool;
  }
  public void setServingTool(ServingTool servingTool) {
    this.servingTool = servingTool;
  }
  
  @ApiModelProperty(value = "Date on which the Serving was deployed", readOnly = true)
  public Date getDeployed() {
    return deployed;
  }
  public void setDeployed(Date deployed) {
    this.deployed = deployed;
  }
  
  @ApiModelProperty(value = "Revision identifier of the last deployment", readOnly = true)
  public String getRevision() {
    return revision;
  }
  public void setRevision(String revision) {
    this.revision = revision;
  }
  
  @ApiModelProperty(value = "Condition of the serving replicas", readOnly = true)
  public ServingStatusCondition getCondition() {
    return condition;
  }
  public void setCondition(ServingStatusCondition condition) {
    this.condition = condition;
  }

  @ApiModelProperty(value = "Resource configuration for predictor", readOnly = true)
  public DeployableComponentResources getPredictorResources() {
    return predictorResources;
  }
  public void setPredictorResources(DeployableComponentResources predictorResources) {
    this.predictorResources = predictorResources;
  }
  
  @ApiModelProperty(value = "Resource configuration for transformer", readOnly = true)
  public DeployableComponentResources getTransformerResources() {
    return transformerResources;
  }
  public void setTransformerResources(DeployableComponentResources transformerResources) {
    this.transformerResources = transformerResources;
  }

  @ApiModelProperty(value = "Request batching configuration for inference", readOnly = true)
  public BatchingConfiguration getBatchingConfiguration() { return batchingConfiguration; }
  public void setBatchingConfiguration(BatchingConfiguration batchingConfiguration) {
    this.batchingConfiguration = batchingConfiguration;
  }
  
  @JsonIgnore
  public ServingWrapper getServingWrapper() {

    ServingWrapper servingWrapper = new ServingWrapper(
        new Serving(id, name, description, modelPath, predictor, transformer, modelName, modelVersion,
          modelFramework, artifactVersion, requestedInstances, requestedTransformerInstances, modelServer, servingTool,
          inferenceLogging, predictorResources, transformerResources, batchingConfiguration));
    servingWrapper.setKafkaTopicDTO(kafkaTopicDTO);

    return servingWrapper;
  }
}
