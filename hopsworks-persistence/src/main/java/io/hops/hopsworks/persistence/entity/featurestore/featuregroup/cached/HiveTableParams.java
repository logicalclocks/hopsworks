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
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;

@Entity
@Table(name = "TABLE_PARAMS", catalog = "metastore")
public class HiveTableParams implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected HiveTableParamsPK hiveTableParamsPK;
  @Lob
  @Size(max = 16777215)
  @Column(name = "PARAM_VALUE")
  private String paramValue;
  @JoinColumn(name = "TBL_ID", referencedColumnName = "TBL_ID", insertable = false, updatable = false)
  @ManyToOne(optional = false)
  private HiveTbls hiveTbls;

  public HiveTableParams() {
  }

  public HiveTableParams(HiveTableParamsPK hiveTableParamsPK) {
    this.hiveTableParamsPK = hiveTableParamsPK;
  }

  public HiveTableParams(long tblId, String paramKey) {
    this.hiveTableParamsPK = new HiveTableParamsPK(tblId, paramKey);
  }

  public HiveTableParamsPK getHiveTableParamsPK() {
    return hiveTableParamsPK;
  }

  public void setHiveTableParamsPK(HiveTableParamsPK hiveTableParamsPK) {
    this.hiveTableParamsPK = hiveTableParamsPK;
  }

  public String getParamValue() {
    return paramValue;
  }

  public void setParamValue(String paramValue) {
    this.paramValue = paramValue;
  }

  public HiveTbls getHiveTbls() {
    return hiveTbls;
  }

  public void setHiveTbls(HiveTbls hiveTbls) {
    this.hiveTbls = hiveTbls;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (hiveTableParamsPK != null ? hiveTableParamsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveTableParams)) {
      return false;
    }
    HiveTableParams other = (HiveTableParams) object;
    if ((this.hiveTableParamsPK == null && other.hiveTableParamsPK != null) ||
        (this.hiveTableParamsPK != null && !this.hiveTableParamsPK.equals(other.hiveTableParamsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveTableParams[ " +
        "hiveTableParamsPK=" + hiveTableParamsPK + " ]";
  }
  
}
