/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "sample_study")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SampleStudy.findAll", query = "SELECT s FROM SampleStudy s"),
    @NamedQuery(name = "SampleStudy.findBySampleId", query = "SELECT s FROM SampleStudy s WHERE s.sampleStudyPK.sampleId = :sampleId"),
    @NamedQuery(name = "SampleStudy.findByStudyId", query = "SELECT s FROM SampleStudy s WHERE s.sampleStudyPK.studyId = :studyId")})
public class SampleStudy implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SampleStudyPK sampleStudyPK;

    public SampleStudy() {
    }

    public SampleStudy(SampleStudyPK sampleStudyPK) {
        this.sampleStudyPK = sampleStudyPK;
    }

    public SampleStudy(int sampleId, int studyId) {
        this.sampleStudyPK = new SampleStudyPK(sampleId, studyId);
    }

    public SampleStudyPK getSampleStudyPK() {
        return sampleStudyPK;
    }

    public void setSampleStudyPK(SampleStudyPK sampleStudyPK) {
        this.sampleStudyPK = sampleStudyPK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (sampleStudyPK != null ? sampleStudyPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SampleStudy)) {
            return false;
        }
        SampleStudy other = (SampleStudy) object;
        if ((this.sampleStudyPK == null && other.sampleStudyPK != null) || (this.sampleStudyPK != null && !this.sampleStudyPK.equals(other.sampleStudyPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.SampleStudy[ sampleStudyPK=" + sampleStudyPK + " ]";
    }
    
}
