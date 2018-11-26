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

package io.hops.hopsworks.common.dao.log.operation;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.metadata.Template;

@Entity
@Table(name = "hopsworks.ops_log")
public class OperationsLog implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "op_id")
  private Integer opId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "op_on")
  private OperationOn opOn;

  @Basic(optional = false)
  @NotNull
  @Column(name = "op_type")
  private OperationType opType;

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private Integer projectId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "dataset_id")
  private Long datasetId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_id")
  private Long inodeId;

  public OperationsLog() {
  }

  public OperationsLog(Dataset dataset, OperationType opType) {
    this.opId = dataset.getId();
    this.opOn = OperationOn.Dataset;
    this.opType = opType;
    this.projectId = dataset.getProject().getId();
    this.datasetId = dataset.getInodeId();
    this.inodeId = dataset.getInodeId();
  }

  public OperationsLog(Project project, OperationType opType) {
    this.opId = project.getId();
    this.opOn = OperationOn.Project;
    this.opType = opType;
    this.projectId = project.getId();
    this.inodeId = project.getInode().getId();
    this.datasetId = -1L;
  }

  public OperationsLog(Project project, Dataset dataset, Template template,
          Inode inode, OperationType opType) {
    this.opId = template.getId();
    this.opOn = OperationOn.Schema;
    this.opType = opType;
    this.projectId = project.getId();
    this.datasetId = dataset.getInodeId();
    this.inodeId = inode.getId();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getOpId() {
    return opId;
  }

  public void setOpId(Integer opId) {
    this.opId = opId;
  }

  public OperationOn getOpOn() {
    return opOn;
  }

  public void setOpOn(OperationOn opOn) {
    this.opOn = opOn;
  }

  public OperationType getOpType() {
    return opType;
  }

  public void setOpType(OperationType opType) {
    this.opType = opType;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public Long getInodeId() {
    return inodeId;
  }

  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }

  public Long getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Long datasetId) {
    this.datasetId = datasetId;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 41 * hash + Objects.hashCode(this.id);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final OperationsLog other = (OperationsLog) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "OperationsLog{" + "id=" + id + ", opId=" + opId + ", opOn=" + opOn
            + ", opType=" + opType + ", projectId=" + projectId + ", datasetId="
            + datasetId + ", inodeId=" + inodeId + '}';
  }

}
