package io.hops.hopsworks.common.dao.pythonDeps;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Version {

  private String version;
  private String status = "Not Installed";

  public Version() {
  }

  public Version(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
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
    if (o instanceof Version == false) {
      return false;
    }
    Version v = (Version) o;
    if (v.version.compareTo(this.version) != 0) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return version.hashCode(); //To change body of generated methods, choose Tools | Templates.
  }

}
