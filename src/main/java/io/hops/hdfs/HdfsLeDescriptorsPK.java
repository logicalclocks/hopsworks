/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.hdfs;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class HdfsLeDescriptorsPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private long id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "partition_val")
    private int partitionVal;

    public HdfsLeDescriptorsPK() {
    }

    public HdfsLeDescriptorsPK(long id, int partitionVal) {
        this.id = id;
        this.partitionVal = partitionVal;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPartitionVal() {
        return partitionVal;
    }

    public void setPartitionVal(int partitionVal) {
        this.partitionVal = partitionVal;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) id;
        hash += (int) partitionVal;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof HdfsLeDescriptorsPK)) {
            return false;
        }
        HdfsLeDescriptorsPK other = (HdfsLeDescriptorsPK) object;
        if (this.id != other.id) {
            return false;
        }
        if (this.partitionVal != other.partitionVal) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.hdfs.HdfsLeDescriptorsPK[ id=" + id + ", partitionVal=" + partitionVal + " ]";
    }
    
}
