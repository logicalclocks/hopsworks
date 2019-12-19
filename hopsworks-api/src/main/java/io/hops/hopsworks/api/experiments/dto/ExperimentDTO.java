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

  private String description;

  private Double metric;

  private String userFullName;

  private String function;

  private String experimentType;

  private String direction;

  private String optimizationKey;

  private String model;

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

  public enum XAttrSetFlag {
    CREATE,
    REPLACE;

    public static XAttrSetFlag fromString(String param) {
      return valueOf(param.toUpperCase());
    }
  }
}
