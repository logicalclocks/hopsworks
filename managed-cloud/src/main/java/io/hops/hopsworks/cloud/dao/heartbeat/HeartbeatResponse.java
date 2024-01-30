/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat;

import io.hops.hopsworks.cloud.CloudNode;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommand;

import java.util.List;

public class HeartbeatResponse extends BaseMessage {
  private final List<CloudNode> workers; //workers contains all nodes not just worker nodes.
  private final List<CloudCommand> commands;
  private final List<String> blockedUsers;

  public HeartbeatResponse(List<CloudNode> workers,
          List<CloudCommand> commands, List<String> blockedUsers) {
    this.workers = workers;
    this.commands = commands;
    this.blockedUsers = blockedUsers;
  }

  public List<CloudNode> getWorkers() {
    return workers;
  }

  public List<CloudCommand> getCommands() {
    return commands;
  }

  public List<String> getBlockedUsers() {
    return blockedUsers;
  }
}
