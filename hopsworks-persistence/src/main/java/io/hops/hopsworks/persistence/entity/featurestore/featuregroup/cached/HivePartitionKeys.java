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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "PARTITION_KEYS", catalog = "metastore")
public class HivePartitionKeys implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected HivePartitionKeysPK hivePartitionKeysPK;
  @Size(max = 4000)
  @Column(name = "PKEY_COMMENT")
  private String pkeyComment;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 767)
  @Column(name = "PKEY_TYPE")
  private String pkeyType;
  @Basic(optional = false)
  @NotNull
  @Column(name = "INTEGER_IDX")
  private int integerIdx;
  @JoinColumn(name = "TBL_ID", referencedColumnName = "TBL_ID", insertable = false, updatable = false)
  @ManyToOne(optional = false)
  private HiveTbls hiveTbls;

  public HivePartitionKeys() {
  }

  public HivePartitionKeys(HivePartitionKeysPK hivePartitionKeysPK) {
    this.hivePartitionKeysPK = hivePartitionKeysPK;
  }

  public HivePartitionKeys(HivePartitionKeysPK hivePartitionKeysPK, String pkeyType, int integerIdx) {
    this.hivePartitionKeysPK = hivePartitionKeysPK;
    this.pkeyType = pkeyType;
    this.integerIdx = integerIdx;
  }

  public HivePartitionKeys(long tblId, String pkeyName) {
    this.hivePartitionKeysPK = new HivePartitionKeysPK(tblId, pkeyName);
  }

  public HivePartitionKeysPK getHivePartitionKeysPK() {
    return hivePartitionKeysPK;
  }

  public void setHivePartitionKeysPK(HivePartitionKeysPK hivePartitionKeysPK) {
    this.hivePartitionKeysPK = hivePartitionKeysPK;
  }

  public String getPkeyComment() {
    return pkeyComment;
  }

  public void setPkeyComment(String pkeyComment) {
    this.pkeyComment = pkeyComment;
  }

  public String getPkeyType() {
    return pkeyType;
  }

  public void setPkeyType(String pkeyType) {
    this.pkeyType = pkeyType;
  }

  public int getIntegerIdx() {
    return integerIdx;
  }

  public void setIntegerIdx(int integerIdx) {
    this.integerIdx = integerIdx;
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
    hash += (hivePartitionKeysPK != null ? hivePartitionKeysPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HivePartitionKeys)) {
      return false;
    }
    HivePartitionKeys other = (HivePartitionKeys) object;
    if ((this.hivePartitionKeysPK == null && other.hivePartitionKeysPK != null) ||
        (this.hivePartitionKeysPK != null && !this.hivePartitionKeysPK.equals(other.hivePartitionKeysPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HivePartitionKeys[ " +
        "hivePartitionKeysPK=" + hivePartitionKeysPK +
        " ]";
  }
  
}
