/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
public class DeployableComponentResources implements Serializable {
  
  public DeployableComponentResources() { }
  
  @JsonSetter(nulls = Nulls.SKIP)
  private DockerResourcesConfiguration requests = DeployableComponentResources.getDefaultRequestsResources();
  
  @JsonSetter(nulls = Nulls.SKIP)
  private DockerResourcesConfiguration limits = DeployableComponentResources.getDefaultLimitsResources();
  
  public DockerResourcesConfiguration getRequests() {
    return requests;
  }
  @JsonSetter(nulls = Nulls.SKIP)
  public void setRequests(DockerResourcesConfiguration requests) {
    this.requests = requests;
  }
  
  public DockerResourcesConfiguration getLimits() {
    return limits;
  }
  @JsonSetter(nulls = Nulls.SKIP)
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
  public int hashCode() {
    return Objects.hash(requests, limits);
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

