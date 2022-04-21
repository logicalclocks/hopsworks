/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat;

import io.hops.hopsworks.cloud.CloudNode;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CommandStatus;

import java.util.List;
import java.util.Map;

public class HeartbeatRequest extends BaseMessage {
  private final List<CloudNode> decommissioningNodes;
  private final List<CloudNode> decommissionedNodes;
  private final Map<String, CommandStatus> commandsStatus;
  private final boolean firstHeartBeat;
  private final long allocatedVcores;
  private final long pendingVcores;
  private final long allocatedMemoryMB;
  private final long pendingMemoryMB; 
  private final long allocatedGPUs;
  private final long pendingGPUs;
  
  public HeartbeatRequest(List<CloudNode> decommissionedNodes, List<CloudNode> decommissioningNodes,
      Map<String, CommandStatus> commandsStatus, long allocatedVcores, long pendingVcores,
      long allocatedMemoryMB, long pendingMemoryMB, long allocatedGPUs, long pendingGPUs) {
    this(decommissionedNodes, decommissioningNodes, commandsStatus, false, allocatedVcores, pendingVcores,
        allocatedMemoryMB, pendingMemoryMB, allocatedGPUs, pendingGPUs);
  }

  public HeartbeatRequest(List<CloudNode> decommissionedNodes, List<CloudNode> decommissioningNodes,
      Map<String, CommandStatus> commandsStatus, boolean firstHeartBeat, long allocatedVcores, long pendingVcores,
      long allocatedMemoryMB, long pendingMemoryMB, long allocatedGPUs, long pendingGPUs) {
    this.decommissionedNodes = decommissionedNodes;
    this.decommissioningNodes = decommissioningNodes;
    this.commandsStatus = commandsStatus;
    this.firstHeartBeat = firstHeartBeat;
    this.allocatedVcores = allocatedVcores;
    this.pendingVcores = pendingVcores;
    this.allocatedMemoryMB = allocatedMemoryMB;
    this.pendingMemoryMB = pendingMemoryMB;
    this.allocatedGPUs = allocatedGPUs;
    this.pendingGPUs = pendingGPUs;
  }

  public List<CloudNode> getDecommissioningNodes() {
    return decommissioningNodes;
  }

  public List<CloudNode> getDecommissionedNodes() {
    return decommissionedNodes;
  }

  public Map<String, CommandStatus> getCommandsStatus() {
    return commandsStatus;
  }
  
  public boolean isFirstHeartBeat(){return firstHeartBeat;}

  public long getAllocatedVcores() {
    return allocatedVcores;
  }

  public long getPendingVcores() {
    return pendingVcores;
  }

  public long getAllocatedMemoryMB() {
    return allocatedMemoryMB;
  }

  public long getPendingMemoryMB() {
    return pendingMemoryMB;
  }
}
