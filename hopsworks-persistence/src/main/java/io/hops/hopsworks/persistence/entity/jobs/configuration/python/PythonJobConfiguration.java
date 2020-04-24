/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.jobs.configuration.python;

import io.hops.hopsworks.persistence.entity.jobs.configuration.DockerJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PythonJobConfiguration extends DockerJobConfiguration {
  
  public PythonJobConfiguration() {
  }
  
  @XmlElement
  private String appPath;
  
  @XmlElement
  private String files;
  
  public String getAppPath() {
    return appPath;
  }
  
  public void setAppPath(String appPath) {
    this.appPath = appPath;
  }
  
  public String getFiles() {
    return files;
  }
  
  
  
  public void setFiles(String files) {
    this.files = files;
  }
  
  @Override
  @XmlElement(name = "jobType")
  public JobType getJobType() {
    return JobType.PYTHON;
  }
  
}
