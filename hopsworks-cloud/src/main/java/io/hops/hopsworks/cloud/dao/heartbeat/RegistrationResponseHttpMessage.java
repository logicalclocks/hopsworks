/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat;

public class RegistrationResponseHttpMessage extends BaseHttpMessage {
  private final RegistrationResponse payload;

  public RegistrationResponseHttpMessage(Integer code, String status, String message, RegistrationResponse payload) {
    super(code, status, message);
    this.payload = payload;
  }

  public RegistrationResponse getPayload() {
    return payload;
  }
}
