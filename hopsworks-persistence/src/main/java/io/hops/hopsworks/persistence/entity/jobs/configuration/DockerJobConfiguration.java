/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.jobs.configuration;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DockerJobConfiguration extends JobConfiguration {

  public DockerJobConfiguration(){}

  @XmlElement
  private int cores = 1;

  @XmlElement
  private int memory = 2048;

  @XmlElement
  private int gpus = 0;

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

  @Override
  @XmlElement(name="jobType")
  public JobType getJobType() {
    return JobType.DOCKER;
  }

}
