/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

public class CommandStatus {
  public enum CLOUD_COMMAND_STATUS {
    NEW,
    ONGOING,
    SUCCEED,
    FAILED;
  }

  private final CLOUD_COMMAND_STATUS status;
  private final String message;

  public CommandStatus(CLOUD_COMMAND_STATUS status, String message) {
    this.status = status;
    this.message = message;
  }

  public CLOUD_COMMAND_STATUS getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public static boolean isFinal(CLOUD_COMMAND_STATUS status) {
    return status.equals(CLOUD_COMMAND_STATUS.SUCCEED) || status.equals(CLOUD_COMMAND_STATUS.FAILED);
  }
}
