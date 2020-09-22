/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat;

import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommand;
import java.util.List;

public class RegistrationResponse extends BaseMessage {
  private final List<CloudCommand> commands;

  public RegistrationResponse(List<CloudCommand> commands) {
    this.commands = commands;
  }

  public List<CloudCommand> getCommands() {
    return commands;
  }
}
