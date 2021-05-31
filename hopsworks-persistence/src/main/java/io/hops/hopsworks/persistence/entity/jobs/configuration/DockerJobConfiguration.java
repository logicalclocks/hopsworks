/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.jobs.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.persistence.entity.jobs.configuration.python.PythonJobConfiguration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({PythonJobConfiguration.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("dockerJobConfiguration")
@JsonSubTypes({
  @JsonSubTypes.Type(value = PythonJobConfiguration.class, name = "PythonJobConfiguration")}
)
public class DockerJobConfiguration extends JobConfiguration {

  public DockerJobConfiguration(){}

  @XmlElement
  private int cores = 1;

  @XmlElement
  private int memory = 2048;

  @XmlElement
  private int gpus = 0;

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
  private String outputPath = "/logs";

  @XmlElement
  private Long uid;

  @XmlElement
  private Long gid;

  public int getCores() {
    return cores;
  }

  public void setCores(int cores) {
    this.cores = cores;
  }

  public int getMemory() {
    return memory;
  }

  public void setMemory(int memory) {
    this.memory = memory;
  }

  public int getGpus() {
    return gpus;
  }

  public void setGpus(int gpus) {
    this.gpus = gpus;
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

  @Override
  @XmlElement(name="jobType")
  public JobType getJobType() {
    return JobType.DOCKER;
  }

}
