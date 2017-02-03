package io.hops.hopsworks.common.dao.pythonDeps;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LibVersions {

  private String channelUrl;
  private String lib;
  private List<Version> versions;
  private String status = "Not Installed";

  public LibVersions() {
  }

  /**
   *
   * @param channelUrl
   * @param lib
   * @param versions
   */
  public LibVersions(String channelUrl, String lib) {
    this.channelUrl = channelUrl;
    this.lib = lib;
  }

  public void addVersion(Version version) {
    if (this.versions == null) {
      this.versions = new ArrayList<>();
    }
    if (!versions.contains(version)) {
      this.versions.add(version);
    }
  }
  
  public void reverseVersionList() {
    this.versions = Lists.reverse(this.versions); 
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

  public List<Version> getVersions() {
    return versions;
  }

  public void setVersions(List<Version> versions) {
    this.versions = versions;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof LibVersions) {
      LibVersions pd = (LibVersions) o;
      if (pd.getChannelUrl().compareToIgnoreCase(this.channelUrl) == 0
              && pd.getLib().compareToIgnoreCase(this.lib) == 0
              && pd.getVersions().size() == this.versions.size()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (this.channelUrl.hashCode() / 3 + this.lib.hashCode()
            + this.versions.hashCode()) / 2;
  }
}
