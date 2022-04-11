/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("deployableComponentResources")
public class DeployableComponentResources {
  
  public DeployableComponentResources(){}
  
  @XmlElement
  private DockerResourcesConfiguration requests = new DockerResourcesConfiguration();
  
  @XmlElement
  private DockerResourcesConfiguration limits = new DockerResourcesConfiguration();
  
  public DockerResourcesConfiguration getRequests() {
    return requests;
  }
  public void setRequests(DockerResourcesConfiguration requests) {
    this.requests = requests;
  }
  
  public DockerResourcesConfiguration getLimits() {
    return limits;
  }
  public void setLimits(DockerResourcesConfiguration limits) {
    this.limits = limits;
  }
  
  @Override
  public final boolean equals(Object object) {
    if (!(object instanceof DeployableComponentResources)) {
      return false;
    }
    DeployableComponentResources other = (DeployableComponentResources) object;
    return this.requests.equals(other.requests) && this.limits.equals(other.limits);
  }
}

