/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving;

import io.hops.hopsworks.common.serving.ServingStatusEnum;

import java.util.List;

public class KubeServingInternalStatus {
  
  private ServingStatusEnum servingStatus;
  private String externalIP;
  private Integer externalPort;
  private List<String> internalIPs;
  private Integer internalPort;
  private String internalPath;
  private Boolean available; // Whether the service is reachable or not. If so, we can perform actions.
  private Integer availableReplicas;
  private Integer availableTransformerReplicas;
  private List<String> conditions;
  
  public KubeServingInternalStatus() {
  }
  
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
  
  public String getExternalIP() {
    return externalIP;
  }
  public void setExternalIP(String externalIP) {
    this.externalIP = externalIP;
  }
  
  public Integer getExternalPort() {
    return externalPort;
  }
  public void setExternalPort(Integer externalPort) {
    this.externalPort = externalPort;
  }
  
  public List<String> getInternalIPs() {
    return internalIPs;
  }
  public void setInternalIPs(List<String> internalIPs) {
    this.internalIPs = internalIPs;
  }
  
  public Integer getInternalPort() {
    return internalPort;
  }
  public void setInternalPort(Integer internalPort) {
    this.internalPort = internalPort;
  }
  
  public String getInternalPath() { return internalPath; }
  public void setInternalPath(String internalPath) { this.internalPath = internalPath; }
  
  public List<String> getConditions() { return conditions; }
  public void setConditions(List<String> conditions) { this.conditions = conditions; }
}
