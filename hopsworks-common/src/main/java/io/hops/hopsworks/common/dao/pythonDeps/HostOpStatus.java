package io.hops.hopsworks.common.dao.pythonDeps;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HostOpStatus {

  private String hostId;
  private String status;

  public HostOpStatus() {
  }

  public HostOpStatus(String hostId, String status) {
    this.hostId = hostId;
    this.status = status;
  }

  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return hostId + ":" + status;
  }
  
  // Two versions are equal if they have the same 'name', status doesn't matter.
  @Override
  public boolean equals(Object o) {
    if (o instanceof HostOpStatus == false) {
      return false;
    }
    HostOpStatus v = (HostOpStatus) o;
    if (v.hostId.equals(this.hostId)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hostId.hashCode() * 17; //To change body of generated methods, choose Tools | Templates.
  }

}
