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

package io.hops.hopsworks.api.models.dto;

import io.hops.hopsworks.common.api.RestDTO;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contains configuration and other information about an experiment
 */
@XmlRootElement
public class ModelDTO extends RestDTO<ModelDTO> {

  public ModelDTO() {
    //Needed for JAXB
  }

  private String id;

  private String name;

  private int version;

  private String userFullName;

  private Long created;

  private ModelResult metrics;

  private String description;

  private String[] environment;

  private String program;

  private String experimentId;

  private String kernelId;

  private String jobName;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getUserFullName() {
    return userFullName;
  }

  public void setUserFullName(String userFullName) {
    this.userFullName = userFullName;
  }

  public ModelResult getMetrics() {
    return metrics;
  }

  public void setMetrics(ModelResult metrics) {
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

  public String getKernelId() {
    return kernelId;
  }

  public void setKernelId(String kernelId) {
    this.kernelId = kernelId;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public enum XAttrSetFlag {
    CREATE,
    REPLACE;

    public static XAttrSetFlag fromString(String param) {
      return valueOf(param.toUpperCase());
    }
  }
}
