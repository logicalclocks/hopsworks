/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.persistence.entity.hdfs;

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
@Table(name = "hops.hdfs_directory_with_quota_feature")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "HdfsDirectoryWithQuotaFeature.findAll",
          query = "SELECT h FROM HdfsDirectoryWithQuotaFeature h"),
  @NamedQuery(name = "HdfsDirectoryWithQuotaFeature.findByInodeId",
          query
          = "SELECT h FROM HdfsDirectoryWithQuotaFeature h WHERE h.inodeId = :inodeId"),
  @NamedQuery(name = "HdfsDirectoryWithQuotaFeature.findByNsquota",
          query
          = "SELECT h FROM HdfsDirectoryWithQuotaFeature h WHERE h.nsquota = :nsquota"),
  @NamedQuery(name = "HdfsDirectoryWithQuotaFeature.findBySsquota",
          query
          = "SELECT h FROM HdfsDirectoryWithQuotaFeature h WHERE h.ssquota = :ssquota"),
  @NamedQuery(name = "HdfsDirectoryWithQuotaFeature.findByNscount",
          query
          = "SELECT h FROM HdfsDirectoryWithQuotaFeature h WHERE h.nscount = :nscount"),
  @NamedQuery(name = "HdfsDirectoryWithQuotaFeature.findByStorageSpace",
          query
          = "SELECT h FROM HdfsDirectoryWithQuotaFeature h WHERE h.storageSpace = :storageSpace")})
public class HdfsDirectoryWithQuotaFeature implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final long MB = 1024l * 1024l;

  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "inodeId")
  private Long inodeId;
  @Column(name = "nsquota")
  private BigInteger nsquota;
  @Column(name = "ssquota")
  private BigInteger ssquota;
  @Column(name = "nscount")
  private BigInteger nscount;
  @Column(name = "storage_space")
  private BigInteger storageSpace;
  @Column(name = "typespace_quota_disk")
  private BigInteger typespaceQuotaDisk = BigInteger.valueOf(-1);
  @Column(name = "typespace_quota_ssd")
  private BigInteger typespaceQuotaSsd = BigInteger.valueOf(-1);
  @Column(name = "typespace_quota_raid5")
  private BigInteger typespaceQuotaRaid5 = BigInteger.valueOf(-1);
  @Column(name = "typespace_quota_archive")
  private BigInteger typespaceQuotaArchive = BigInteger.valueOf(-1);
  @Column(name = "typespace_used_disk")
  private BigInteger typespaceUsedDisk = BigInteger.valueOf(-1);
  @Column(name = "typespace_used_ssd")
  private BigInteger typespaceUsedSsd = BigInteger.valueOf(-1);
  @Column(name = "typespace_used_raid5")
  private BigInteger typespaceUsedRaid5 = BigInteger.valueOf(-1);
  @Column(name = "typespace_used_archive")
  private BigInteger typespaceUsedArchive = BigInteger.valueOf(-1);

  public HdfsDirectoryWithQuotaFeature() {
  }

  public HdfsDirectoryWithQuotaFeature(Long inodeId) {
    this.inodeId = inodeId;
  }

  public Long getInodeId() {
    return inodeId;
  }

  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }

  public BigInteger getNsquota() {
    return nsquota;
  }

  public void setNsquota(BigInteger nsquota) {
    this.nsquota = nsquota;
  }

  public BigInteger getSsquota() {
    return ssquota;
  }

  public long getSsquotaInMBs() {
    long quota = ssquota.longValue();
    quota /= MB;
    return quota;
  }

  public void setSsquota(BigInteger ssquota) {
    this.ssquota = ssquota;
  }

  public BigInteger getNscount() {
    return nscount;
  }

  public void setNscount(BigInteger nscount) {
    this.nscount = nscount;
  }

  public BigInteger getStorageSpace() {
    return storageSpace;
  }

  public long getStorageSpaceInMBs() {
    long quota = storageSpace.longValue();
    quota /= MB;
    return quota;
  }

  public void setStorageSpaceInMBs(long storageSpaceInMBs) {
    this.storageSpace = BigInteger.valueOf(storageSpaceInMBs * MB);
  }

  public void setStorageSpace(BigInteger storageSpace) {
    this.storageSpace = storageSpace;
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
    if (!(object instanceof HdfsDirectoryWithQuotaFeature)) {
      return false;
    }
    HdfsDirectoryWithQuotaFeature other = (HdfsDirectoryWithQuotaFeature) object;
    if ((this.inodeId == null && other.inodeId != null) || (this.inodeId != null
            && !this.inodeId.equals(other.inodeId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.hdfs.fileoperations.HdfsDirectoryWithQuotaFeature[ inodeId="
            + inodeId + " ]";
  }

  public BigInteger getTypespaceQuotaDisk() {
    return typespaceQuotaDisk;
  }

  public void setTypespaceQuotaDisk(BigInteger typespaceQuotaDisk) {
    this.typespaceQuotaDisk = typespaceQuotaDisk;
  }

  public BigInteger getTypespaceQuotaSsd() {
    return typespaceQuotaSsd;
  }

  public void setTypespaceQuotaSsd(BigInteger typespaceQuotaSsd) {
    this.typespaceQuotaSsd = typespaceQuotaSsd;
  }

  public BigInteger getTypespaceQuotaRaid5() {
    return typespaceQuotaRaid5;
  }

  public void setTypespaceQuotaRaid5(BigInteger typespaceQuotaRaid5) {
    this.typespaceQuotaRaid5 = typespaceQuotaRaid5;
  }

  public BigInteger getTypespaceQuotaArchive() {
    return typespaceQuotaArchive;
  }

  public void setTypespaceQuotaArchive(BigInteger typespaceQuotaArchive) {
    this.typespaceQuotaArchive = typespaceQuotaArchive;
  }

  public BigInteger getTypespaceUsedDisk() {
    return typespaceUsedDisk;
  }

  public void setTypespaceUsedDisk(BigInteger typespaceUsedDisk) {
    this.typespaceUsedDisk = typespaceUsedDisk;
  }

  public BigInteger getTypespaceUsedSsd() {
    return typespaceUsedSsd;
  }

  public void setTypespaceUsedSsd(BigInteger typespaceUsedSsd) {
    this.typespaceUsedSsd = typespaceUsedSsd;
  }

  public BigInteger getTypespaceUsedRaid5() {
    return typespaceUsedRaid5;
  }

  public void setTypespaceUsedRaid5(BigInteger typespaceUsedRaid5) {
    this.typespaceUsedRaid5 = typespaceUsedRaid5;
  }

  public BigInteger getTypespaceUsedArchive() {
    return typespaceUsedArchive;
  }

  public void setTypespaceUsedArchive(BigInteger typespaceUsedArchive) {
    this.typespaceUsedArchive = typespaceUsedArchive;
  }
}
