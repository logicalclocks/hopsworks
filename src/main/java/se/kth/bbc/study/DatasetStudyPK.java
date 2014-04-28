/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author roshan
 */
@Embeddable
public class DatasetStudyPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "Dataset_id")
    private int datasetid;
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private int id;

    public DatasetStudyPK() {
    }

    public DatasetStudyPK(int datasetid, int id) {
        this.datasetid = datasetid;
        this.id = id;
    }

    public int getDatasetid() {
        return datasetid;
    }

    public void setDatasetid(int datasetid) {
        this.datasetid = datasetid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) datasetid;
        hash += (int) id;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetStudyPK)) {
            return false;
        }
        DatasetStudyPK other = (DatasetStudyPK) object;
        if (this.datasetid != other.datasetid) {
            return false;
        }
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.DatasetStudyPK[ datasetid=" + datasetid + ", id=" + id + " ]";
    }
    
}
