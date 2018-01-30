package io.hops.hopsworks.common.dao.kagent;

import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Status;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServiceStatusDTO {

  private static final long serialVersionUID = 1L;
  private String group;
  private String service;
  private Status status;

  public ServiceStatusDTO() {
  }

  public ServiceStatusDTO(String group, String service, Status status) {
    this.service = service;
    this.group = group;
    this.status = status;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Health getHealth() {
    if (status == Status.Failed || status == Status.Stopped) {
      return Health.Bad;
    }
    return Health.Good;
  }
}
