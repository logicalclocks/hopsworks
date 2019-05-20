/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.dao.maggy;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class MaggyDriverPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 30)
  @Column(name = "app_id")
  private String appId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "port")
  private int port;

  public MaggyDriverPK() {
  }

  public MaggyDriverPK(String appId, int port) {
    this.appId = appId;
    this.port = port;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (appId != null ? appId.hashCode() : 0);
    hash += (int) port;
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof MaggyDriverPK)) {
      return false;
    }
    MaggyDriverPK other = (MaggyDriverPK) object;
    if ((this.appId == null && other.appId != null) || (this.appId != null && !this.appId.equals(other.appId))) {
      return false;
    }
    if (this.port != other.port) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.maggy.MaggyDriverPK[ appId=" + appId + ", port=" + port + " ]";
  }
  
}
