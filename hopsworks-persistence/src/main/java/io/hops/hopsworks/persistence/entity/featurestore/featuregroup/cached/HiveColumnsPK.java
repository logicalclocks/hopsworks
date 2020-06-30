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
public class HiveColumnsPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "CD_ID")
  private long cdId;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 767)
  @Column(name = "COLUMN_NAME")
  private String columnName;

  public HiveColumnsPK() {
  }

  public HiveColumnsPK(long cdId, String columnName) {
    this.cdId = cdId;
    this.columnName = columnName;
  }

  public long getCdId() {
    return cdId;
  }

  public void setCdId(long cdId) {
    this.cdId = cdId;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) cdId;
    hash += (columnName != null ? columnName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveColumnsPK)) {
      return false;
    }
    HiveColumnsPK other = (HiveColumnsPK) object;
    if (this.cdId != other.cdId) {
      return false;
    }
    if ((this.columnName == null && other.columnName != null) ||
        (this.columnName != null && !this.columnName.equals(other.columnName))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveColumnsPK[ cdId=" + cdId + ", columnName=" + columnName +
        " ]";
  }
  
}
