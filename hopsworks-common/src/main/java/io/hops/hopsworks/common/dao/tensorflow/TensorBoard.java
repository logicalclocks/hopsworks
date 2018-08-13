/*
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
 */

package io.hops.hopsworks.common.dao.tensorflow;

import io.hops.hopsworks.common.dao.project.Project;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.EmbeddedId;
import javax.persistence.Column;
import javax.persistence.Basic;
import javax.persistence.Temporal;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "tensorboard", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "TensorBoard.findAll", query = "SELECT t FROM TensorBoard t")
        , @NamedQuery(name = "TensorBoard.findByProjectId", query = "SELECT t FROM TensorBoard t WHERE " +
        "t.tensorBoardPK.projectId = :projectId")
        , @NamedQuery(name = "TensorBoard.findByTeamMember", query = "SELECT t FROM TensorBoard t WHERE " +
        "t.tensorBoardPK.email = :email")
        , @NamedQuery(name = "TensorBoard.findByProjectAndUser", query = "SELECT t FROM TensorBoard t WHERE " +
        "t.tensorBoardPK.projectId = :projectId AND t.tensorBoardPK.email = :email")})
public class TensorBoard implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  private TensorBoardPK tensorBoardPK;

  @Basic(optional = false)
  @NotNull
  @Column(name = "hdfs_user_id")
  private int hdfsUserId;

  @Column(name = "pid")
  private BigInteger pid;

  @Size(min = 1,
          max = 100)
  @Column(name = "endpoint")
  private String endpoint;

  @Size(min = 1,
          max = 100)
  @Column(name = "elastic_id")
  private String elasticId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "last_accessed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastAccessed;

  @Size(min = 1,
          max = 10000)
  @Column(name = "hdfs_logdir")
  private String hdfsLogdir;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Project project;

  public TensorBoard() {
  }

  public TensorBoard(TensorBoardPK tensorBoardPK) {
    this.setTensorBoardPK(tensorBoardPK);
  }

  public TensorBoard(TensorBoardPK tensorBoardPK, BigInteger pid, String endpoint, String elasticId,
                           Date lastAccessed, String hdfsLogdir) {
    this.setTensorBoardPK(tensorBoardPK);
    this.setPid(pid);
    this.setEndpoint(endpoint);
    this.setElasticId(elasticId);
    this.setLastAccessed(lastAccessed);
    this.setHdfsLogdir(hdfsLogdir);
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public TensorBoardPK getTensorBoardPK() {
    return tensorBoardPK;
  }

  public void setTensorBoardPK(TensorBoardPK tensorBoardPK) {
    this.tensorBoardPK = tensorBoardPK;
  }

  public BigInteger getPid() {
    return pid;
  }

  public void setPid(BigInteger pid) {
    this.pid = pid;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Date getLastAccessed() {
    return lastAccessed;
  }

  public void setLastAccessed(Date lastAccessed) {
    this.lastAccessed = lastAccessed;
  }

  public String getHdfsLogdir() {
    return hdfsLogdir;
  }

  public void setHdfsLogdir(String hdfsLogdir) {
    this.hdfsLogdir = hdfsLogdir;
  }

  public int getHdfsUserId() {
    return hdfsUserId;
  }

  public void setHdfsUserId(int hdfsUserId) {
    this.hdfsUserId = hdfsUserId;
  }

  public String getElasticId() {
    return elasticId;
  }

  public void setElasticId(String elasticId) {
    this.elasticId = elasticId;
  }
}
