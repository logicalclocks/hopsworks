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

package io.hops.hopsworks.api.experiments.dto;

import io.hops.hopsworks.api.experiments.dto.results.ExperimentResultSummaryDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardDTO;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contains configuration and other information about an experiment
 */
@XmlRootElement
public class ExperimentDTO extends RestDTO<ExperimentDTO> {

  public ExperimentDTO() {
    //Needed for JAXB
  }

  private String id;

  private Long started;

  private Long finished;

  private String state;

  private String name;
  
  private String projectName;

  private String description;

  private Double metric;

  private String userFullName;

  private String function;

  private String experimentType;

  private String direction;

  private String optimizationKey;
  
  private String model;
  
  private String modelProjectName;

  private String jobName;

  private Long duration;

  private String appId;

  private String bestDir;

  private String[] environment;

  private String program;

  private String kernelId;

  private ExperimentResultSummaryDTO results;

  private TensorBoardDTO tensorboard;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getStarted() {
    return started;
  }

  public void setStarted(Long started) {
    this.started = started;
  }

  public Long getFinished() {
    return finished;
  }

  public void setFinished(Long finished) {
    this.finished = finished;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Double getMetric() {
    return metric;
  }

  public void setMetric(Double metric) {
    this.metric = metric;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getFunction() {
    return function;
  }

  public void setFunction(String function) {
    this.function = function;
  }

  public String getExperimentType() {
    return experimentType;
  }

  public void setExperimentType(String experimentType) {
    this.experimentType = experimentType;
  }

  public TensorBoardDTO getTensorboard() {
    return tensorboard;
  }

  public void setTensorboard(TensorBoardDTO tensorboard) {
    this.tensorboard = tensorboard;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getOptimizationKey() {
    return optimizationKey;
  }

  public void setOptimizationKey(String optimizationKey) {
    this.optimizationKey = optimizationKey;
  }

  public ExperimentResultSummaryDTO getResults() {
    return results;
  }

  public void setResults(ExperimentResultSummaryDTO results) {
    this.results = results;
  }
  
  public String getModel() {
    return model;
  }
  
  public void setModel(String model) {
    this.model = model;
  }
  
  public String getModelProjectName() {
    return modelProjectName;
  }
  
  public void setModelProjectName(String modelProjectName) {
    this.modelProjectName = modelProjectName;
  }
  
  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getBestDir() {
    return bestDir;
  }

  public void setBestDir(String bestDir) {
    this.bestDir = bestDir;
  }

  public String getProgram() {
    return program;
  }

  public void setProgram(String program) {
    this.program = program;
  }

  public String[] getEnvironment() {
    return environment;
  }

  public void setEnvironment(String[] environment) {
    this.environment = environment;
  }

  public String getKernelId() {
    return kernelId;
  }

  public void setKernelId(String kernelId) {
    this.kernelId = kernelId;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public static ExperimentDTO mergeExperiment(ExperimentDTO e1, ExperimentDTO e2) {
    ExperimentDTO experiment = new ExperimentDTO();
    if(e2 == null) {
      e2 = new ExperimentDTO();
    }
    experiment.setId(mergeValues(e1.getId(), e2.getId()));
    experiment.setStarted(mergeValues(e1.getStarted(), e2.getStarted()));
    experiment.setFinished(mergeValues(e1.getFinished(), e2.getFinished()));
    experiment.setState(mergeValues(e1.getState(), e2.getState()));
    experiment.setName(mergeValues(e1.getName(), e2.getName()));
    experiment.setProjectName(mergeValues(e1.getProjectName(), e2.getProjectName()));
    experiment.setDescription(mergeValues(e1.getDescription(), e2.getDescription()));
    experiment.setMetric(mergeValues(e1.getMetric(), e2.getMetric()));
    experiment.setUserFullName(mergeValues(e1.getUserFullName(), e2.getUserFullName()));
    experiment.setFunction(mergeValues(e1.getFunction(), e2.getFunction()));
    experiment.setExperimentType(mergeValues(e1.getExperimentType(), e2.getExperimentType()));
    experiment.setDirection(mergeValues(e1.getDirection(), e2.getDirection()));
    experiment.setOptimizationKey(mergeValues(e1.getOptimizationKey(), e2.getOptimizationKey()));
    experiment.setModel(mergeValues(e1.getModel(), e2.getModel()));
    experiment.setModelProjectName(mergeValues(e1.getModelProjectName(), e1.getModelProjectName()));
    experiment.setJobName(mergeValues(e1.getJobName(), e2.getJobName()));
    experiment.setDuration(mergeValues(e1.getDuration(), e2.getDuration()));
    experiment.setAppId(mergeValues(e1.getAppId(), e2.getAppId()));
    experiment.setBestDir(mergeValues(e1.getBestDir(), e2.getBestDir()));
    experiment.setEnvironment(mergeValues(e1.getEnvironment(), e2.getEnvironment()));
    experiment.setProgram(mergeValues(e1.getProgram(), e2.getProgram()));
    experiment.setKernelId(mergeValues(e1.getKernelId(), e2.getKernelId()));
    experiment.setResults(mergeValues(e1.getResults(), e2.getResults()));
    experiment.setTensorboard(mergeValues(e1.getTensorboard(), e2.getTensorboard()));
    return experiment;
  }
  
  private static <O> O mergeValues(O v1, O v2) {
    return v1 == null ? v2 : v1;
  }
}
