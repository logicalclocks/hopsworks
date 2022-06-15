/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.hops.hopsworks.common.serving.ServingStatusEnum;

import java.util.List;

public class KubeServingInternalStatus {
  
  private ServingStatusEnum servingStatus;
  private String hopsworksInferencePath; // Hopsworks REST API
  private String modelServerInferencePath; // Model server (Ingress gateway)
  private Boolean available; // Whether the service is reachable or not. If so, we can perform actions.
  private Integer availableReplicas;
  private Integer availableTransformerReplicas;
  private List<String> conditions;
  
  public KubeServingInternalStatus() {
  }
  
  // Status
  
  public ServingStatusEnum getServingStatus() {
    return servingStatus;
  }
  public void setServingStatus(ServingStatusEnum servingStatus){
    this.servingStatus = servingStatus;
  }
  
  public List<String> getConditions() { return conditions; }
  public void setConditions(List<String> conditions) { this.conditions = conditions; }
  
  public Boolean getAvailable() { return available; }
  public void setAvailable(Boolean available) { this.available = available; }
  
  // Replicas
  
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
  
  // Inference path
  
  public String getHopsworksInferencePath() { return hopsworksInferencePath; }
  public void setHopsworksInferencePath(String hopsworksInferencePath) {
    this.hopsworksInferencePath = hopsworksInferencePath;
  }
  
  public String getModelServerInferencePath() { return modelServerInferencePath; }
  public void setModelServerInferencePath(String modelServerInferencePath) {
    this.modelServerInferencePath = modelServerInferencePath;
  }
}
