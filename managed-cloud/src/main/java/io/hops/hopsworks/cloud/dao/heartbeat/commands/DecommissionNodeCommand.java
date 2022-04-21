/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

public class DecommissionNodeCommand extends CloudCommand {

  private final String host;
  private final String nodeId;

  public DecommissionNodeCommand(String id, String host, String nodeId) {
    super(id, CloudCommandType.DECOMMISSION_NODE);
    this.host = host;
    this.nodeId = nodeId;
  }

  public String getHost() {
    return host;
  }

  public String getNodeId() {
    return nodeId;
  }

  @Override
  public String toString() {
    return "DecommissionNodesCommand{" + "host=" + host + "nodeId=" + nodeId + '}';
  }
}
