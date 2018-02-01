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
