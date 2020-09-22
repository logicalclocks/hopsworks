/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

public abstract class CloudCommand {

  private final Long id;
  private final CloudCommandType type;

  public CloudCommand(Long id, CloudCommandType type) {
    this.id = id;
    this.type = type;
  }

  public Long getId() {
    return id;
  }

  public CloudCommandType getType() {
    return type;
  }
}
