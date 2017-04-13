package io.hops.hopsworks.common.maintenance;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Maintenance implements Serializable {

  private static final long serialVersionUID = 1L;

  private short status;

  private String message;

  private String otp = "true";

  public Maintenance() {
    message = "Administration message";
  }

  public short getStatus() {
    return status;
  }

  public void setStatus(short status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getOtp() {
    return otp;
  }

  public void setOtp(String otp) {
    this.otp = otp;
  }
}
