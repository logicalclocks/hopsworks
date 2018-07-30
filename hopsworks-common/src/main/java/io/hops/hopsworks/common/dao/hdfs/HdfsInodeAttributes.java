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
