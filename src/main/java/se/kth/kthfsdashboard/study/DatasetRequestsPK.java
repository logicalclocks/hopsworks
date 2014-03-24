/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

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
public class DatasetRequestsPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "study_id")
    private int studyId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "dataset_id")
    private int datasetId;

    public DatasetRequestsPK() {
    }

    public DatasetRequestsPK(int studyId, int datasetId) {
        this.studyId = studyId;
        this.datasetId = datasetId;
    }

    public int getStudyId() {
        return studyId;
    }

    public void setStudyId(int studyId) {
        this.studyId = studyId;
    }

    public int getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(int datasetId) {
        this.datasetId = datasetId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) studyId;
        hash += (int) datasetId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetRequestsPK)) {
            return false;
        }
        DatasetRequestsPK other = (DatasetRequestsPK) object;
        if (this.studyId != other.studyId) {
            return false;
        }
        if (this.datasetId != other.datasetId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.DatasetRequestsPK[ studyId=" + studyId + ", datasetId=" + datasetId + " ]";
    }
    
}
