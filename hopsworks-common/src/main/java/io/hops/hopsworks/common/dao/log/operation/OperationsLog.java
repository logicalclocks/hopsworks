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
  private Integer datasetId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_id")
  private Integer inodeId;

  public OperationsLog() {
  }

  public OperationsLog(Dataset dataset, OperationType opType) {
    this.opId = dataset.getId();
    this.opOn = OperationOn.Dataset;
    this.opType = opType;
    this.projectId = dataset.getProject().getId();
    this.datasetId = dataset.geInodeId();
    this.inodeId = dataset.geInodeId();
  }

  public OperationsLog(Project project, OperationType opType) {
    this.opId = project.getId();
    this.opOn = OperationOn.Project;
    this.opType = opType;
    this.projectId = project.getId();
    this.inodeId = project.getInode().getId();
    this.datasetId = -1;
  }

  public OperationsLog(Project project, Dataset dataset, Template template,
          Inode inode, OperationType opType) {
    this.opId = template.getId();
    this.opOn = OperationOn.Schema;
    this.opType = opType;
    this.projectId = project.getId();
    this.datasetId = dataset.geInodeId();
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

  public Integer getInodeId() {
    return inodeId;
  }

  public void setInodeId(Integer inodeId) {
    this.inodeId = inodeId;
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
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
