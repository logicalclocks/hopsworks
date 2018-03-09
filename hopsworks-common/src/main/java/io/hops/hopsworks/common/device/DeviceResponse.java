package io.hops.hopsworks.common.device;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DeviceResponse {

  private String message;

  private Integer code;

  private String reason;

  private String jwt;

  public DeviceResponse(){
  }

  public DeviceResponse(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public DeviceResponse(Integer code, String message, String reason) {
    this.code = code;
    this.message = message;
    this.reason = reason;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getJwt() {
    return jwt;
  }

  public void setJwt(String jwt) {
    this.jwt = jwt;
  }
}
