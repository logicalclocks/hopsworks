/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.hops.hopsworks.common.serving.ServingStatusEnum;

public class KubeServingInternalStatus {
  
  private ServingStatusEnum servingStatus;
  private Integer nodePort;
  private Integer availableReplicas;
  
  public ServingStatusEnum getServingStatus() {
    return servingStatus;
  }
  
  public void setServingStatus(ServingStatusEnum servingStatus){
    this.servingStatus = servingStatus;
  }
  
  public Integer getAvailableReplicas() {
    return availableReplicas;
  }
  
  public void setAvailableReplicas(Integer availableReplicas) {
    this.availableReplicas = availableReplicas;
  }
  
  public Integer getNodePort() {
    return nodePort;
  }
  
  public void setNodePort(Integer nodePort) {
    this.nodePort = nodePort;
  }
}
