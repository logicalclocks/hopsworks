/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

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
