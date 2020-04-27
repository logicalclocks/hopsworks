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

import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "SDS", catalog = "metastore")
public class HiveSds implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "SD_ID")
  private Long sdId;
  @Size(max = 4000)
  @Column(name = "INPUT_FORMAT")
  private String inputFormat;
  @Size(max = 4000)
  @Column(name = "LOCATION")
  private String location;
  @Size(max = 4000)
  @Column(name = "OUTPUT_FORMAT")
  private String outputFormat;
  @JoinColumns({
      @JoinColumn(name = "PARENT_ID", referencedColumnName = "parent_id"),
      @JoinColumn(name = "NAME", referencedColumnName = "name"),
      @JoinColumn(name = "PARTITION_ID", referencedColumnName = "partition_id")})
  @ManyToOne(optional = false)
  private Inode inode;
  @JoinColumn(name = "CD_ID", referencedColumnName = "CD_ID")
  @ManyToOne
  private HiveCds cdId;

  public HiveSds() {
  }

  public HiveSds(Long sdId) {
    this.sdId = sdId;
  }

  public Long getSdId() {
    return sdId;
  }

  public void setSdId(Long sdId) {
    this.sdId = sdId;
  }

  public String getInputFormat() {
    return inputFormat;
  }

  public void setInputFormat(String inputFormat) {
    this.inputFormat = inputFormat;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public HiveCds getCdId() {
    return cdId;
  }

  public void setCdId(HiveCds cdId) {
    this.cdId = cdId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (sdId != null ? sdId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HiveSds)) {
      return false;
    }
    HiveSds other = (HiveSds) object;
    if ((this.sdId == null && other.sdId != null) || (this.sdId != null && !this.sdId.equals(other.sdId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.project.HiveSds[ sdId=" + sdId + " ]";
  }
  
}
