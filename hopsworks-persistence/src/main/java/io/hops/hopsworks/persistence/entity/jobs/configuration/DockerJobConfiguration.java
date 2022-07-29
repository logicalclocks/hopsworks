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

package io.hops.hopsworks.persistence.entity.jobs.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.persistence.entity.serving.DockerResourcesConfiguration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("dockerJobConfiguration")
public class DockerJobConfiguration extends JobConfiguration {

  public DockerJobConfiguration(){}

  @XmlElement
  private DockerResourcesConfiguration resourceConfig = new DockerResourcesConfiguration();

  @XmlElement
  private String imagePath;

  @XmlElement
  private List<String> volumes;

  @XmlElement
  private List<String> envVars;

  @XmlElement
  private List<String> command;

  @XmlElement
  private List<String> inputPaths;

  @XmlElement
  private String outputPath;

  @XmlElement
  private Long uid;

  @XmlElement
  private Long gid;

  @XmlElement
  private Boolean logRedirection = true;

  public DockerResourcesConfiguration getResourceConfig() {
    return resourceConfig;
  }

  public void setResourceConfig(DockerResourcesConfiguration resourceConfig) {
    this.resourceConfig = resourceConfig;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public List<String> getVolumes() {
    return volumes;
  }

  public void setVolumes(List<String> volumes) {
    this.volumes = volumes;
  }

  public List<String> getEnvVars() {
    return envVars;
  }

  public void setEnvVars(List<String> envVars) {
    this.envVars = envVars;
  }

  public List<String> getCommand() {
    return command;
  }

  public void setCommand(List<String> command) {
    this.command = command;
  }

  public List<String> getInputPaths() {
    return inputPaths;
  }

  public void setInputPaths(List<String> inputPaths) {
    this.inputPaths = inputPaths;
  }

  public String getOutputPath() {
    return outputPath;
  }

  public void setOutputPath(String outputPath) {
    this.outputPath = outputPath;
  }

  public Long getUid() {
    return uid;
  }

  public void setUid(Long uid) {
    this.uid = uid;
  }

  public Long getGid() {
    return gid;
  }

  public void setGid(Long gid) {
    this.gid = gid;
  }

  public Boolean getLogRedirection() {
    return logRedirection;
  }

  public void setLogRedirection(Boolean logRedirection) {
    this.logRedirection = logRedirection;
  }

  @Override
  @XmlElement(name="jobType")
  public JobType getJobType() {
    return JobType.DOCKER;
  }

}
