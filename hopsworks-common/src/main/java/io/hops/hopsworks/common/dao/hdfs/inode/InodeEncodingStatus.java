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

package io.hops.hopsworks.common.dao.hdfs.inode;

import java.io.Serializable;
import java.math.BigInteger;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * It is used to discover if a file is compressed or not
 */
@Entity
@Table(name = "hops.hdfs_encoding_status")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "InodeEncodingStatus.findAll",
          query
          = "SELECT i FROM InodeEncodingStatus i"),
  @NamedQuery(name = "InodeEncodingStatus.findById",
          query
          = "SELECT i FROM InodeEncodingStatus i WHERE i.inodeId = :id")})
public class InodeEncodingStatus implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "inode_id")
  private Integer inodeId;

  @Column(name = "status")
  private Integer status;

  @Size(max = 8)
  @Column(name = "codec")
  private String codec;

  @Column(name = "target_replication")
  private Integer targetReplication;

  @Column(name = "parity_status")
  private BigInteger parityStatus;

  @Column(name = "status_modification_time")
  private BigInteger statusModificationTime;

  @Column(name = "parity_status_modification_time")
  private BigInteger parityStatusModificationTime;

  @Column(name = "parity_inode_id")
  private BigInteger parityInodeId;

  @Size(max = 36)
  @Column(name = "parity_file_name")
  private char clientMachine;

  @Column(name = "lost_blocks")
  private Integer lostBlocks;

  @Column(name = "lost_parity_blocks")
  private Integer lostParityBlocks;

  @Column(name = "revoked")
  private boolean revoked;

  public InodeEncodingStatus() {

  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public void setCodec(String codec) {
    this.codec = codec;
  }

  public void setTargetReplication(Integer targetReplication) {
    this.targetReplication = targetReplication;
  }

  public void setParityStatus(BigInteger parityStatus) {
    this.parityStatus = parityStatus;
  }

  public void setStatusModificationTime(BigInteger statusModificationTime) {
    this.statusModificationTime = statusModificationTime;
  }

  public void setParityStatusModificationTime(
          BigInteger parityStatusModificationTime) {
    this.parityStatusModificationTime = parityStatusModificationTime;
  }

  public void setParityInodeId(BigInteger parityInodeId) {
    this.parityInodeId = parityInodeId;
  }

  public void setClientMachine(char clientMachine) {
    this.clientMachine = clientMachine;
  }

  public void setLostBlocks(Integer lostBlocks) {
    this.lostBlocks = lostBlocks;
  }

  public void setLostParityBlocks(Integer lostParityBlocks) {
    this.lostParityBlocks = lostParityBlocks;
  }

  public void setRevoked(boolean revoked) {
    this.revoked = revoked;
  }

  public Integer getInodeId() {
    return this.inodeId;
  }

  public Integer getStatus() {
    return status;
  }

  public String getCodec() {
    return codec;
  }

  public Integer getTargetReplication() {
    return targetReplication;
  }

  public BigInteger getParityStatus() {
    return parityStatus;
  }

  public BigInteger getStatusModificationTime() {
    return statusModificationTime;
  }

  public BigInteger getParityStatusModificationTime() {
    return parityStatusModificationTime;
  }

  public BigInteger getParityInodeId() {
    return parityInodeId;
  }

  public char getClientMachine() {
    return clientMachine;
  }

  public Integer getLostBlocks() {
    return lostBlocks;
  }

  public Integer getLostParityBlocks() {
    return lostParityBlocks;
  }

  public boolean isRevoked() {
    return revoked;
  }

}
