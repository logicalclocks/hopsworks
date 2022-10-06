/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerResourcesConfiguration {

  public DockerResourcesConfiguration(){}

  public DockerResourcesConfiguration(double cores, int memory, int gpus) {
    this.cores = cores;
    this.memory = memory;
    this.gpus = gpus;
  }
  
  private double cores = 1;
  
  private int memory = 2048;

  private int gpus = 0;

  public double getCores() {
    return cores;
  }

  public void setCores(double cores) {
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
