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
public class HivePartitionKeysPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "TBL_ID")
  private long tblId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(name = "PKEY_NAME")
  private String pkeyName;

  public HivePartitionKeysPK() {
  }

  public HivePartitionKeysPK(long tblId, String pkeyName) {
    this.tblId = tblId;
    this.pkeyName = pkeyName;
  }

  public long getTblId() {
    return tblId;
  }

  public void setTblId(long tblId) {
    this.tblId = tblId;
  }

  public String getPkeyName() {
    return pkeyName;
  }

  public void setPkeyName(String pkeyName) {
    this.pkeyName = pkeyName;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) tblId;
    hash += (pkeyName != null ? pkeyName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HivePartitionKeysPK)) {
      return false;
    }
    HivePartitionKeysPK other = (HivePartitionKeysPK) object;
    if (this.tblId != other.tblId) {
      return false;
    }
    if ((this.pkeyName == null && other.pkeyName != null) ||
        (this.pkeyName != null && !this.pkeyName.equals(other.pkeyName))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HivePartitionKeysPK[ tblId=" + tblId + ", " +
        "pkeyName=" + pkeyName +
        " ]";
  }
  
}
