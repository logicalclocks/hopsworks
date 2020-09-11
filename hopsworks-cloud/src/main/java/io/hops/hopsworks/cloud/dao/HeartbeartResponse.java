/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao;

import io.hops.hopsworks.cloud.CloudNode;

import java.util.List;
import java.util.Map;

public class HeartbeartResponse {
  private List<CloudNode> workers;
  private Map<String, Integer> removeRequest;

  public HeartbeartResponse(List<CloudNode> workers) {
    this.workers = workers;
  }

  public List<CloudNode> getWorkers() {
    return workers;
  }

  public void setWorkers(List<CloudNode> workers) {
    this.workers = workers;
  }

  public Map<String, Integer> getRemoveRequest() {
    return removeRequest;
  }

  public void setRemoveRequest(Map<String, Integer> removeRequest) {
    this.removeRequest = removeRequest;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Worker nodes:\n");
    for (CloudNode node : workers) {
      sb.append(node.toString()).append("\n");
    }

    sb.append("=============\n");
    sb.append("Remove nodes request:\n");
    for (Map.Entry<String, Integer> rr : removeRequest.entrySet()) {
      sb.append("Removing ").append(rr.getValue()).append(" of type ").append(rr.getKey()).append("\n");
    }
    return sb.toString();
  }
}
