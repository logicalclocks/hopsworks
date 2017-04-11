package io.hops.hopsworks.common.dao.pythonDeps;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PythonDepJson {

  private String channelUrl;
  private String lib;
  private String version;
  private String status = "Not Installed";
  private String preinstalled = "false";

  public PythonDepJson() {
  }
  /**
   * 
   * @param channelUrl
   * @param lib
   * @param version 
   */
  public PythonDepJson(String channelUrl, String lib, String version, String 
          preinstalled) {
    this.channelUrl = channelUrl;
    this.lib = lib;
    this.version = version;
    this.preinstalled = preinstalled;
  }

  public PythonDepJson(PythonDep pd) {
    this.channelUrl = pd.getRepoUrl().getUrl();
    this.lib = pd.getDependency();
    this.version = pd.getVersion();
    this.status = pd.getStatus().toString();
    this.preinstalled = Boolean.toString(pd.isPreinstalled());
  }

  public String getChannelUrl() {
    return channelUrl;
  }

  public void setChannelUrl(String channelUrl) {
    this.channelUrl = channelUrl;
  }

  public String getLib() {
    return lib;
  }

  public void setLib(String lib) {
    this.lib = lib;
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

  public String getPreinstalled() {
    return preinstalled;
  }

  public void setPreinstalled(String preinstalled) {
    this.preinstalled = preinstalled;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof PythonDepJson) {
      PythonDepJson pd = (PythonDepJson) o;
      if (pd.getChannelUrl().compareToIgnoreCase(this.channelUrl) == 0
              && pd.getLib().compareToIgnoreCase(this.lib) == 0
              && pd.getVersion().compareToIgnoreCase(this.version) == 0) {
        return true;
      }
    }
    if (o instanceof PythonDep) {
      PythonDep pd = (PythonDep) o;
      if (pd.getRepoUrl().getUrl().compareToIgnoreCase(this.channelUrl) == 0
              && pd.getDependency().compareToIgnoreCase(this.lib) == 0
              && pd.getVersion().compareToIgnoreCase(this.version) == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (this.channelUrl.hashCode() / 3 + this.lib.hashCode()
            + this.version.hashCode()) / 2;
  }
}
