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
public class SampleStudyPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "sample_id")
    private int sampleId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "study_id")
    private int studyId;

    public SampleStudyPK() {
    }

    public SampleStudyPK(int sampleId, int studyId) {
        this.sampleId = sampleId;
        this.studyId = studyId;
    }

    public int getSampleId() {
        return sampleId;
    }

    public void setSampleId(int sampleId) {
        this.sampleId = sampleId;
    }

    public int getStudyId() {
        return studyId;
    }

    public void setStudyId(int studyId) {
        this.studyId = studyId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) sampleId;
        hash += (int) studyId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SampleStudyPK)) {
            return false;
        }
        SampleStudyPK other = (SampleStudyPK) object;
        if (this.sampleId != other.sampleId) {
            return false;
        }
        if (this.studyId != other.studyId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.SampleStudyPK[ sampleId=" + sampleId + ", studyId=" + studyId + " ]";
    }
    
}
