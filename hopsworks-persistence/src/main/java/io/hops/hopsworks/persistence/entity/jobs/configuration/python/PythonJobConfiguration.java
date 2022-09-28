/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.jobs.configuration.python;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.persistence.entity.jobs.configuration.DockerJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonTypeName("pythonJobConfiguration")
public class PythonJobConfiguration extends DockerJobConfiguration {
  @XmlElement
  private String appPath;
  @XmlElement
  private String files;
  
  public PythonJobConfiguration() {
  }
  
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
