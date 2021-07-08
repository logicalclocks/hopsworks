/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.hops.hopsworks.common.serving.ServingStatusEnum;

public class KubeServingInternalStatus {
  
  private ServingStatusEnum servingStatus;
  private Integer nodePort;
  private Boolean available; // Whether the service is reachable or not. If so, we can perform actions.
  private Integer availableReplicas;
  private Integer availableTransformerReplicas;
  
  public ServingStatusEnum getServingStatus() {
    return servingStatus;
  }
  
  public void setServingStatus(ServingStatusEnum servingStatus){
    this.servingStatus = servingStatus;
  }
  
  public Boolean getAvailable() { return available; }
  
  public void setAvailable(Boolean available) { this.available = available; }
  
  public Integer getAvailableReplicas() {
    return availableReplicas;
  }
  
  public void setAvailableReplicas(Integer availableReplicas) {
    this.availableReplicas = availableReplicas;
  }
  
  public Integer getAvailableTransformerReplicas() {
    return availableTransformerReplicas;
  }
  
  public void setAvailableTransformerReplicas(Integer availableReplicas) {
    this.availableTransformerReplicas = availableReplicas;
  }
  
  public Integer getNodePort() {
    return nodePort;
  }
  
  public void setNodePort(Integer nodePort) {
    this.nodePort = nodePort;
  }
}
