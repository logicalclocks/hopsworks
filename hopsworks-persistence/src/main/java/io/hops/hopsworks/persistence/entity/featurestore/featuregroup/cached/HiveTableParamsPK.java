/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class HiveTableParamsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "TBL_ID")
  private long tblId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 256)
  @Column(name = "PARAM_KEY")
  private String paramKey;

  public HiveTableParamsPK() {
  }

  public HiveTableParamsPK(long tblId, String paramKey) {
    this.tblId = tblId;
    this.paramKey = paramKey;
  }

  public long getTblId() {
    return tblId;
  }

  public void setTblId(long tblId) {
    this.tblId = tblId;
  }

  public String getParamKey() {
    return paramKey;
  }

  public void setParamKey(String paramKey) {
    this.paramKey = paramKey;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) tblId;
    hash += (paramKey != null ? paramKey.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveTableParamsPK)) {
      return false;
    }
    HiveTableParamsPK other = (HiveTableParamsPK) object;
    if (this.tblId != other.tblId) {
      return false;
    }
    if ((this.paramKey == null && other.paramKey != null) ||
        (this.paramKey != null && !this.paramKey.equals(other.paramKey))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveTableParamsPK[ tblId=" + tblId + ", paramKey=" + paramKey +
        " ]";
  }
  
}
