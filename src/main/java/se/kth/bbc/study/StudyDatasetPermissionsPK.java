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
public class StudyDatasetPermissionsPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "study_id")
    private int studyId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "dataset_id")
    private int datasetId;

    public StudyDatasetPermissionsPK() {
    }

    public StudyDatasetPermissionsPK(int studyId, int datasetId) {
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
        if (!(object instanceof StudyDatasetPermissionsPK)) {
            return false;
        }
        StudyDatasetPermissionsPK other = (StudyDatasetPermissionsPK) object;
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
        return "se.kth.kthfsdashboard.study.StudyDatasetPermissionsPK[ studyId=" + studyId + ", datasetId=" + datasetId + " ]";
    }
    
}
