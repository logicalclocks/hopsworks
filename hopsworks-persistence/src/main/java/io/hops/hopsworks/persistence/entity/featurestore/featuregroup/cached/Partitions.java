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
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "PARTITIONS", catalog = "metastore", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Partitions.findByTblId", query = "SELECT p FROM Partitions p WHERE p.tableId = :tblId")})
public class Partitions implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "PART_ID")
  private Long partId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "CREATE_TIME")
  private int createTime;
  @Basic(optional = false)
  @NotNull
  @Column(name = "LAST_ACCESS_TIME")
  private int lastAccessTime;
  @Size(max = 767)
  @Column(name = "PART_NAME")
  private String partName;
  @Column(name = "TBL_ID")
  private Long tableId;

  public Partitions() {
  }

  public Partitions(Long partId) {
    this.partId = partId;
  }

  public Partitions(Long partId, int createTime, int lastAccessTime) {
    this.partId = partId;
    this.createTime = createTime;
    this.lastAccessTime = lastAccessTime;
  }

  public Long getPartId() {
    return partId;
  }

  public void setPartId(Long partId) {
    this.partId = partId;
  }

  public int getCreateTime() {
    return createTime;
  }

  public void setCreateTime(int createTime) {
    this.createTime = createTime;
  }

  public int getLastAccessTime() {
    return lastAccessTime;
  }

  public void setLastAccessTime(int lastAccessTime) {
    this.lastAccessTime = lastAccessTime;
  }

  public String getPartName() {
    return partName;
  }

  public void setPartName(String partName) {
    this.partName = partName;
  }

  public Long getTableId() {
    return tableId;
  }

  public void setTableId(Long tableId) {
    this.tableId = tableId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (partId != null ? partId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Partitions)) {
      return false;
    }
    Partitions other = (Partitions) object;
    if ((this.partId == null && other.partId != null) || (this.partId != null && !this.partId.equals(other.partId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.dataset.Partitions[ partId=" + partId + " ]";
  }
  
}
