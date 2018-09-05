/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.common;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStatus;

import java.util.List;

public class KubeAppStatus {

  private DeploymentStatus deploymentStatus;
  private Service service;
  private List<Pod> podList;

  public KubeAppStatus(DeploymentStatus deploymentStatus, Service service, List<Pod> podList) {
    this.deploymentStatus = deploymentStatus;
    this.service = service;
    this.podList = podList;
  }

  public DeploymentStatus getDeploymentStatus() {
    return deploymentStatus;
  }

  public void setDeploymentStatus(DeploymentStatus deploymentStatus) {
    this.deploymentStatus = deploymentStatus;
  }

  public Service getService() {
    return service;
  }

  public void setService(Service service) {
    this.service = service;
  }

  public Integer getAvailableReplicas() {
    return deploymentStatus.getAvailableReplicas();
  }

  public List<Pod> getPodList() {
    return podList;
  }

  public void setPodList(List<Pod> podList) {
    this.podList = podList;
  }

  public boolean isDeploymentActive() {
    return deploymentStatus != null || service != null;
  }
}