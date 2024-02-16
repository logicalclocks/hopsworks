/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DockerResourcesConfiguration implements Serializable
{

  public DockerResourcesConfiguration(){}

  public DockerResourcesConfiguration(double cores, int memory, int gpus) {
    this.cores = cores;
    this.memory = memory;
    this.gpus = gpus;
  }
  
  @JsonSetter(nulls = Nulls.SKIP)
  private double cores = 1;
  
  @JsonSetter(nulls = Nulls.SKIP)
  private int memory = 2048;
  
  @JsonSetter(nulls = Nulls.SKIP)
  private int gpus = 0;

  public double getCores() {
    return cores;
  }
  
  @JsonSetter(nulls = Nulls.SKIP)
  public void setCores(double cores) {
    this.cores = cores;
  }

  public int getMemory() {
    return memory;
  }
  
  @JsonSetter(nulls = Nulls.SKIP)
  public void setMemory(int memory) {
    this.memory = memory;
  }

  public int getGpus() {
    return gpus;
  }
  
  @JsonSetter(nulls = Nulls.SKIP)
  public void setGpus(int gpus) {
    this.gpus = gpus;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(cores, memory, gpus);
  }
  
  @Override
  public final boolean equals(Object object) {
    if (!(object instanceof DockerResourcesConfiguration)) {
      return false;
    }
    DockerResourcesConfiguration other = (DockerResourcesConfiguration) object;
    if (this.cores != other.cores || this.memory != other.memory || this.gpus != other.gpus) {
      return false;
    }
    return true;
  }
}
