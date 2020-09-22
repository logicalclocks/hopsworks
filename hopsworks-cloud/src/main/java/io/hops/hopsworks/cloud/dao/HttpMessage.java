/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao;

import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatResponse;

public class HttpMessage {
  private final String status;
  private final Integer code;
  private final String message;
  private HeartbeatResponse payload;

  public HttpMessage(String status, Integer code, String message, HeartbeatResponse payload) {
    this.status = status;
    this.code = code;
    this.message = message;
    this.payload = payload;
  }

  public String getStatus() {
    return status;
  }

  public Integer getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public HeartbeatResponse getResponse() {
    return payload;
  }
}
