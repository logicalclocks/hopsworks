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

package io.hops.hopsworks.common.dao.hdfs;

import java.io.Serializable;
import java.math.BigInteger;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hops.hdfs_inode_attributes")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "HdfsInodeAttributes.findAll",
          query = "SELECT h FROM HdfsInodeAttributes h"),
  @NamedQuery(name = "HdfsInodeAttributes.findByInodeId",
          query
          = "SELECT h FROM HdfsInodeAttributes h WHERE h.inodeId = :inodeId"),
  @NamedQuery(name = "HdfsInodeAttributes.findByNsquota",
          query
          = "SELECT h FROM HdfsInodeAttributes h WHERE h.nsquota = :nsquota"),
  @NamedQuery(name = "HdfsInodeAttributes.findByDsquota",
          query
          = "SELECT h FROM HdfsInodeAttributes h WHERE h.dsquota = :dsquota"),
  @NamedQuery(name = "HdfsInodeAttributes.findByNscount",
          query
          = "SELECT h FROM HdfsInodeAttributes h WHERE h.nscount = :nscount"),
  @NamedQuery(name = "HdfsInodeAttributes.findByDiskspace",
          query
          = "SELECT h FROM HdfsInodeAttributes h WHERE h.diskspace = :diskspace")})
public class HdfsInodeAttributes implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final long MB = 1024l * 1024l;

  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "inodeId")
  private Integer inodeId;
  @Column(name = "nsquota")
  private BigInteger nsquota;
  @Column(name = "dsquota")
  private BigInteger dsquota;
  @Column(name = "nscount")
  private BigInteger nscount;
  @Column(name = "diskspace")
  private BigInteger diskspace;

  public HdfsInodeAttributes() {
  }

  public HdfsInodeAttributes(Integer inodeId) {
    this.inodeId = inodeId;
  }

  public Integer getInodeId() {
    return inodeId;
  }

  public void setInodeId(Integer inodeId) {
    this.inodeId = inodeId;
  }

  public BigInteger getNsquota() {
    return nsquota;
  }

  public void setNsquota(BigInteger nsquota) {
    this.nsquota = nsquota;
  }

  public BigInteger getDsquota() {
    return dsquota;
  }

  public long getDsquotaInMBs() {
    long quota = dsquota.longValue();
    quota /= MB;
    return quota;
  }

  public void setDsquota(BigInteger dsquota) {
    this.dsquota = dsquota;
  }

  public BigInteger getNscount() {
    return nscount;
  }

  public void setNscount(BigInteger nscount) {
    this.nscount = nscount;
  }

  public BigInteger getDiskspace() {
    return diskspace;
  }

  public long getDiskspaceInMBs() {
    long quota = diskspace.longValue();
    quota /= MB;
    return quota;
  }

  public void setDiskspaceInMBs(long diskspaceInMBs) {
    this.diskspace = BigInteger.valueOf(diskspaceInMBs * MB);
  }

  public void setDiskspace(BigInteger diskspace) {
    this.diskspace = diskspace;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (inodeId != null ? inodeId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HdfsInodeAttributes)) {
      return false;
    }
    HdfsInodeAttributes other = (HdfsInodeAttributes) object;
    if ((this.inodeId == null && other.inodeId != null) || (this.inodeId != null
            && !this.inodeId.equals(other.inodeId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.hdfs.fileoperations.HdfsInodeAttributes[ inodeId="
            + inodeId + " ]";
  }

}
