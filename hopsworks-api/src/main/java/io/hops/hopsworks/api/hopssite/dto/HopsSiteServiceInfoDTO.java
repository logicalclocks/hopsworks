package io.hops.hopsworks.api.hopssite.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HopsSiteServiceInfoDTO {

  private String name;
  private int status;
  private String msg;

  public HopsSiteServiceInfoDTO() {
  }

  public HopsSiteServiceInfoDTO(String name, int status, String msg) {
    this.name = name;
    this.status = status;
    this.msg = msg;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  @Override
  public String toString() {
    return "Service{" + "name=" + name + ", status=" + status + ", msg=" + msg + '}';
  }

}
