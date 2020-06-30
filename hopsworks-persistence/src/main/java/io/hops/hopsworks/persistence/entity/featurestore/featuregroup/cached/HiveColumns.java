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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "COLUMNS_V2", catalog = "metastore")
public class HiveColumns implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected HiveColumnsPK hiveColumnsPK;
  @Size(max = 256)
  @Column(name = "COMMENT")
  private String comment;
  @Lob
  @Size(max = 16777215)
  @Column(name = "TYPE_NAME")
  private String typeName;
  @Basic(optional = false)
  @NotNull
  @Column(name = "INTEGER_IDX")
  private int integerIdx;
  @JoinColumn(name = "CD_ID", referencedColumnName = "CD_ID", insertable = false, updatable = false)
  @ManyToOne(optional = false)
  private HiveCds hiveCds;

  public HiveColumns() {
  }

  public HiveColumns(HiveColumnsPK hiveColumnsPK) {
    this.hiveColumnsPK = hiveColumnsPK;
  }

  public HiveColumns(HiveColumnsPK hiveColumnsPK, int integerIdx) {
    this.hiveColumnsPK = hiveColumnsPK;
    this.integerIdx = integerIdx;
  }

  public HiveColumns(long cdId, String columnName) {
    this.hiveColumnsPK = new HiveColumnsPK(cdId, columnName);
  }

  public HiveColumnsPK getHiveColumnsPK() {
    return hiveColumnsPK;
  }

  public void setHiveColumnsPK(HiveColumnsPK hiveColumnsPK) {
    this.hiveColumnsPK = hiveColumnsPK;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public int getIntegerIdx() {
    return integerIdx;
  }

  public void setIntegerIdx(int integerIdx) {
    this.integerIdx = integerIdx;
  }

  public HiveCds getHiveCds() {
    return hiveCds;
  }

  public void setHiveCds(HiveCds hiveCds) {
    this.hiveCds = hiveCds;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (hiveColumnsPK != null ? hiveColumnsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveColumns)) {
      return false;
    }
    HiveColumns other = (HiveColumns) object;
    if ((this.hiveColumnsPK == null && other.hiveColumnsPK != null) ||
        (this.hiveColumnsPK != null && !this.hiveColumnsPK.equals(other.hiveColumnsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveColumns[ hiveColumnsPK=" + hiveColumnsPK + " ]";
  }
  
}
