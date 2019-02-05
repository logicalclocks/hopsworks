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
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hops.hdfs_le_descriptors")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "HdfsLeDescriptors.findEndpoint",
          query
          = "SELECT h FROM HdfsLeDescriptors h ORDER BY h.hdfsLeDescriptorsPK.id ASC"),
  @NamedQuery(name = "HdfsLeDescriptors.findAll",
          query = "SELECT h FROM HdfsLeDescriptors h"),
  @NamedQuery(name = "HdfsLeDescriptors.findById",
          query
          = "SELECT h FROM HdfsLeDescriptors h WHERE h.hdfsLeDescriptorsPK.id = :id"),
  @NamedQuery(name = "HdfsLeDescriptors.findByCounter",
          query = "SELECT h FROM HdfsLeDescriptors h WHERE h.counter = :counter"),
  @NamedQuery(name
          = "HdfsLeDescriptors.findByHost.jpql.parser.IdentificationVariablname",
          query
          = "SELECT h FROM HdfsLeDescriptors h WHERE h.rpcAddresses = :rpcAddresses"),
  @NamedQuery(name = "HdfsLeDescriptors.findByHttpAddress",
          query
          = "SELECT h FROM HdfsLeDescriptors h WHERE h.httpAddress = :httpAddress"),
  @NamedQuery(name = "HdfsLeDescriptors.findByPartitionVal",
          query
          = "SELECT h FROM HdfsLeDescriptors h WHERE h.hdfsLeDescriptorsPK.partitionVal = :partitionVal")})
public class HdfsLeDescriptors implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected HdfsLeDescriptorsPK hdfsLeDescriptorsPK;
  @Basic(optional = false)
  @NotNull
  @Column(name = "counter")
  private long counter;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 25)
  @Column(name = "rpc_addresses")
  private String rpcAddresses;
  @Size(max = 100)
  @Column(name = "http_address")
  private String httpAddress;

  public HdfsLeDescriptors() {
  }

  public HdfsLeDescriptors(HdfsLeDescriptorsPK hdfsLeDescriptorsPK) {
    this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
  }

  public HdfsLeDescriptors(HdfsLeDescriptorsPK hdfsLeDescriptorsPK, long counter,
          String rpcAddresses) {
    this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
    this.counter = counter;
    this.rpcAddresses = rpcAddresses;
  }

  public HdfsLeDescriptors(long id, int partitionVal) {
    this.hdfsLeDescriptorsPK = new HdfsLeDescriptorsPK(id, partitionVal);
  }

  public HdfsLeDescriptorsPK getHdfsLeDescriptorsPK() {
    return hdfsLeDescriptorsPK;
  }

  public void setHdfsLeDescriptorsPK(HdfsLeDescriptorsPK hdfsLeDescriptorsPK) {
    this.hdfsLeDescriptorsPK = hdfsLeDescriptorsPK;
  }

  public long getCounter() {
    return counter;
  }

  public void setCounter(long counter) {
    this.counter = counter;
  }

  public String getHostname() {
    if (rpcAddresses == null) {
      return "";
    }
    int pos = rpcAddresses.indexOf(",");
    if (pos == -1) {
      return "";
    }
    return rpcAddresses.substring(0, pos);
  }
  
  public String getRpcAddresses() {
    return rpcAddresses;
  }

  public void setRpcAddresses(String rpcAddresses) {
    this.rpcAddresses = rpcAddresses;
  }

  public String getHttpAddress() {
    return httpAddress;
  }

  public void setHttpAddress(String httpAddress) {
    this.httpAddress = httpAddress;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (hdfsLeDescriptorsPK != null ? hdfsLeDescriptorsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof HdfsLeDescriptors)) {
      return false;
    }
    HdfsLeDescriptors other = (HdfsLeDescriptors) object;
    if ((this.hdfsLeDescriptorsPK == null && other.hdfsLeDescriptorsPK != null)
            || (this.hdfsLeDescriptorsPK != null && !this.hdfsLeDescriptorsPK.
            equals(other.hdfsLeDescriptorsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hdfs.HdfsLeDescriptors[ hdfsLeDescriptorsPK="
            + hdfsLeDescriptorsPK + " ]";
  }

}
