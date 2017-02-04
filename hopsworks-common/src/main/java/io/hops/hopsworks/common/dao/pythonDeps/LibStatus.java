package io.hops.hopsworks.common.dao.pythonDeps;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LibStatus {

  private String channelUrl;
  private String lib;
  private String version;
  private String status = "Not Installed";
  private List<HostLibStatus> hosts = new ArrayList<>();

  public LibStatus() {
  }

  public LibStatus(String channelUrl, String lib, String version) {
    this.channelUrl = channelUrl;
    this.lib = lib;
    this.version = version;
  }

  public List<HostLibStatus> getHosts() {
    return hosts;
  }

  public String getStatus() {
    return status;
  }

  public void setHosts(List<HostLibStatus> hosts) {
    this.hosts = hosts;
  }

  public void setStatus(String status) {
    this.status = status;
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

  @Override
  public boolean equals(Object o) {
    if (o instanceof LibStatus) {
      LibStatus pd = (LibStatus) o;
      if (pd.getChannelUrl().compareToIgnoreCase(this.channelUrl) == 0
              && pd.getLib().compareToIgnoreCase(this.lib) == 0
              && pd.getVersion() == this.version) {
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
