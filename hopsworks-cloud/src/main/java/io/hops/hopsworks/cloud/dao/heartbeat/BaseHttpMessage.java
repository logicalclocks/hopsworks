/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat;

public abstract class BaseHttpMessage {
  private final Integer code;
  private final String status;
  private final String message;

  public BaseHttpMessage(Integer code, String status, String message) {
    this.code = code;
    this.status = status;
    this.message = message;
  }

  public Integer getCode() {
    return code;
  }

  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }
}
