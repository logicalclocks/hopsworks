/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

import java.util.Map;

public class RemoveNodesCommand extends CloudCommand {
  private final Map<String, Integer> nodesToRemove;

  public RemoveNodesCommand(String id, Map<String, Integer> nodesToRemove) {
    super(id, CloudCommandType.REMOVE_NODES);
    this.nodesToRemove = nodesToRemove;
  }

  public Map<String, Integer> getNodesToRemove() {
    return nodesToRemove;
  }
  
  @Override
  public String toString() {
    return "RemoveNodesCommand{" +
        "nodesToRemove=" + nodesToRemove +
        '}';
  }
}
