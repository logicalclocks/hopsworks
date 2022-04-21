/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommand;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandType;

public class DummyCommand extends CloudCommand {
  public static final CloudCommandType DUMMY_COMMAND = CloudCommandType.of("DUMMY_COMMAND");

  private final String arguments;

  public DummyCommand(String id, String arguments) {
    super(id, CloudCommandType.of("DUMMY_COMMAND"));
    this.arguments = arguments;
  }

  public String getArguments() {
    return arguments;
  }
}
