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

package io.hops.hopsworks.api.modelregistry.models.dto;

import io.hops.hopsworks.api.dataset.inode.InodeDTO;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Contains configuration and other information about a model
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ModelDTO extends RestDTO<ModelDTO> {

  public ModelDTO() {
    //Needed for JAXB
  }

  private String id;

  private String name;

  private Integer version;
  
  private String projectName;

  private String userFullName;

  private InodeDTO inputExample;

  private String framework;

  private InodeDTO modelSchema;

  private Long created;

  @XmlAnyAttribute
  private HashMap<QName, Double> metrics;

  private String description;

  private String[] environment;

  private String program;
  
  private String experimentId;
  
  private String experimentProjectName;

  private TrainingDatasetDTO trainingDataset;

  private Integer modelRegistryId;

  private TagsDTO tags;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  public String getUserFullName() {
    return userFullName;
  }

  public void setUserFullName(String userFullName) {
    this.userFullName = userFullName;
  }

  public HashMap<QName, Double> getMetrics() {
    return metrics;
  }

  public void setMetrics(HashMap<QName, Double> metrics) {
    this.metrics = metrics;
  }

  public Long getCreated() {
    return created;
  }

  public void setCreated(Long created) {
    this.created = created;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String[] getEnvironment() {
    return environment;
  }

  public void setEnvironment(String[] environment) {
    this.environment = environment;
  }

  public String getProgram() {
    return program;
  }

  public void setProgram(String program) {
    this.program = program;
  }
  
  public String getExperimentId() {
    return experimentId;
  }
  
  public void setExperimentId(String experimentId) {
    this.experimentId = experimentId;
  }
  
  public String getExperimentProjectName() {
    return experimentProjectName;
  }
  
  public void setExperimentProjectName(String experimentProjectName) {
    this.experimentProjectName = experimentProjectName;
  }

  public InodeDTO getInputExample() {
    return inputExample;
  }

  public void setInputExample(InodeDTO inputExample) {
    this.inputExample = inputExample;
  }

  public InodeDTO getModelSchema() {
    return modelSchema;
  }

  public void setModelSchema(InodeDTO modelSchema) {
    this.modelSchema = modelSchema;
  }

  public String getFramework() {
    return framework;
  }

  public void setFramework(String framework) {
    this.framework = framework;
  }

  public TrainingDatasetDTO getTrainingDataset() {
    return trainingDataset;
  }

  public void setTrainingDataset(TrainingDatasetDTO trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  public Integer getModelRegistryId() {
    return modelRegistryId;
  }

  public void setModelRegistryId(Integer modelRegistryId) {
    this.modelRegistryId = modelRegistryId;
  }

  public TagsDTO getTags() {
    return tags;
  }

  public void setTags(TagsDTO tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return "ModelDTO{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", version=" + version +
      ", projectName='" + projectName + '\'' +
      ", userFullName='" + userFullName + '\'' +
      ", inputExample=" + inputExample +
      ", framework='" + framework + '\'' +
      ", modelSchema=" + modelSchema +
      ", created=" + created +
      ", metrics=" + metrics +
      ", description='" + description + '\'' +
      ", environment=" + Arrays.toString(environment) +
      ", program='" + program + '\'' +
      ", experimentId='" + experimentId + '\'' +
      ", experimentProjectName='" + experimentProjectName + '\'' +
      ", trainingDataset=" + trainingDataset +
      ", modelRegistryId=" + modelRegistryId +
      ", tags=" + tags +
      '}';
  }
}
