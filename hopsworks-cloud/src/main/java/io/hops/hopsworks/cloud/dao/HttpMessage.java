/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao;

public class HttpMessage {
  private String status;
  private Integer code;
  private String message;
  private HeartbeartResponse payload;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public HeartbeartResponse getResponse() {
    return payload;
  }

  public void setResponse(HeartbeartResponse response) {
    this.payload = response;
  }
}
