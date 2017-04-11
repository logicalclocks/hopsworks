package io.hops.hopsworks.common.dao.hdfs.inode;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class InodePK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "parent_id")
  private int parentId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @NotNull
  @Column(name = "partition_id")
  private int partitionId;

  public InodePK() {
  }

  public InodePK(int parentId, String name, int partitionId) {
    this.parentId = parentId;
    this.name = name;
    this.partitionId = partitionId;
  }

  public int getParentId() {
    return parentId;
  }

  public void setParentId(int parentId) {
    this.parentId = parentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getPartitionId() {
    return partitionId;
  }

  public void setPartitionId(int partitionId) {
    this.partitionId = partitionId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (int) parentId;
    hash += (name != null ? name.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof InodePK)) {
      return false;
    }
    InodePK other = (InodePK) object;
    if (this.parentId != other.parentId) {
      return false;
    }
    if ((this.name == null && other.name != null) || (this.name != null
            && !this.name.equals(other.name))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.project.fb.InodePK[ parentId=" + parentId + ", name="
            + name + " ]";
  }

}
