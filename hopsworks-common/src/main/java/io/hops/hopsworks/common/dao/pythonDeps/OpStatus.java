package io.hops.hopsworks.common.dao.pythonDeps;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OpStatus {

  private String channelUrl = "default";
  private String lib;
  private String version;
  private String op;
  private String status = "Not Installed";
  private List<HostOpStatus> hosts = new ArrayList<>();

  public OpStatus() {
  }

  public OpStatus(String op, String channelUrl, String lib, String version) {
    this.op = op;
    this.channelUrl = channelUrl;
    this.lib = lib;
    this.version = version;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public List<HostOpStatus> getHosts() {
    return hosts;
  }

  public String getStatus() {
    return status;
  }

  public void setHosts(List<HostOpStatus> hosts) {
    this.hosts = hosts;
  }

  public void addHost(HostOpStatus host) {
    this.hosts.add(host);
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
  public String toString() {
    StringBuffer sb = new StringBuffer("[");
    sb.append(channelUrl)
        .append(",").append(lib)
        .append(",").append(version)
        .append(",").append(op)
        .append(",").append(status)
        .append(",(");
    hosts.forEach((h) -> {
        sb.append(h.toString());
      });
    sb.append(")]");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof OpStatus) {
      OpStatus pd = (OpStatus) o;
      if (pd.getChannelUrl().compareToIgnoreCase(this.channelUrl) == 0
          && pd.getLib().compareToIgnoreCase(this.lib) == 0
          && pd.getVersion().compareTo(this.version) == 0) {
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
