package io.hops.hopsworks.common.dao.pythonDeps;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HostLibStatus {

  private int hostId;
  private String status;

  public HostLibStatus() {
  }

  public HostLibStatus(int hostId, String status) {
    this.hostId = hostId;
    this.status = status;
  }

  public int getHostId() {
    return hostId;
  }

  public void setHostId(int hostId) {
    this.hostId = hostId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  // Two versions are equal if they have the same 'name', status doesn't matter.
  @Override
  public boolean equals(Object o) {
    if (o instanceof HostLibStatus == false) {
      return false;
    }
    HostLibStatus v = (HostLibStatus) o;
    if (v.hostId != this.hostId) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hostId * 17; //To change body of generated methods, choose Tools | Templates.
  }

}
