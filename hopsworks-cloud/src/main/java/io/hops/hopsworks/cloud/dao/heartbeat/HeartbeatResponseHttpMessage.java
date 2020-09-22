/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat;

public class HeartbeatResponseHttpMessage extends BaseHttpMessage {
  private final HeartbeatResponse payload;

  public HeartbeatResponseHttpMessage(Integer code, String status, String message, HeartbeatResponse payload) {
    super(code, status, message);
    this.payload = payload;
  }

  public HeartbeatResponse getPayload() {
    return payload;
  }
}
