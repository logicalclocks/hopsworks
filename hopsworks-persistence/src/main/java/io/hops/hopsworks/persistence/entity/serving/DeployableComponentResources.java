/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployableComponentResources {
  
  public DeployableComponentResources() { }
  
  @XmlElement
  private DockerResourcesConfiguration requests = DeployableComponentResources.getDefaultRequestsResources();
  
  @XmlElement
  private DockerResourcesConfiguration limits = DeployableComponentResources.getDefaultLimitsResources();
  
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

  public static DockerResourcesConfiguration getDefaultRequestsResources() {
    return new DockerResourcesConfiguration(0.2, 32, 0);
  }

  public static DockerResourcesConfiguration getDefaultLimitsResources() {
    return new DockerResourcesConfiguration(-1, -1, -1);
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

