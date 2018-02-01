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
