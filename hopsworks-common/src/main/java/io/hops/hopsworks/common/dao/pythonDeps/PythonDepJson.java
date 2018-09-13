/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.pythonDeps;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PythonDepJson {

  private String channelUrl;
  private String installType;
  private String machineType;
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
   * @param status
   */
  public PythonDepJson(String channelUrl, String installType, String machineType, String lib, String version, String
          preinstalled, String status) {
    this.channelUrl = channelUrl;
    this.lib = lib;
    this.version = version;
    this.preinstalled = preinstalled;
    this.status = status;
    this.installType = installType;
    this.machineType = machineType;
  }

  public PythonDepJson(String channelUrl, String installType, String machineType, String lib, String version, String
          preinstalled) {
    this(channelUrl, installType, machineType, lib, version, preinstalled, "Not Installed");
  }
  
  public PythonDepJson(PythonDep pd) {
    this.channelUrl = pd.getRepoUrl().getUrl();
    this.lib = pd.getDependency();
    this.version = pd.getVersion();
    this.status = pd.getStatus().toString();
    this.installType = pd.getInstallType().name();
    this.machineType = pd.getMachineType().name();
    this.preinstalled = Boolean.toString(pd.isPreinstalled());
  }

  public String getChannelUrl() {
    return channelUrl;
  }

  public void setChannelUrl(String channelUrl) {
    this.channelUrl = channelUrl;
  }

  public String getInstallType() {
    return installType;
  }

  public void setInstallType(String installType) {
    this.installType = installType;
  }

  public String getMachineType() {
    return machineType;
  }

  public void setMachineType(String machineType) {
    this.machineType = machineType;
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
              && pd.getInstallType().compareToIgnoreCase(this.installType) == 0
              && pd.getLib().compareToIgnoreCase(this.lib) == 0
              && pd.getVersion().compareToIgnoreCase(this.version) == 0
              && pd.getMachineType().compareToIgnoreCase(this.machineType) == 0
              && pd.getPreinstalled().compareToIgnoreCase(this.preinstalled) == 0) {
        return true;
      }
    }
    if (o instanceof PythonDep) {
      PythonDep pd = (PythonDep) o;
      if (pd.getRepoUrl().getUrl().compareToIgnoreCase(this.channelUrl) == 0
              && pd.getInstallType().toString().compareToIgnoreCase(this.installType) == 0
              && pd.getDependency().compareToIgnoreCase(this.lib) == 0
              && pd.getVersion().compareToIgnoreCase(this.version) == 0
              && pd.getMachineType().name().compareToIgnoreCase(this.machineType) == 0
              && Boolean.toString(pd.isPreinstalled()).compareToIgnoreCase(this.preinstalled) == 0) {
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
