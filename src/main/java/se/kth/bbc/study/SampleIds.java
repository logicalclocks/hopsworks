/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "SampleIds")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SampleIds.findAll", query = "SELECT s FROM SampleIds s"),
    @NamedQuery(name = "SampleIds.findById", query = "SELECT s FROM SampleIds s WHERE s.sampleIdsPK.id = :id"),
    @NamedQuery(name = "SampleIds.findByStudyName", query = "SELECT s FROM SampleIds s WHERE s.sampleIdsPK.studyName = :studyName")})
public class SampleIds implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SampleIdsPK sampleIdsPK;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sampleIds")
    private Collection<SampleFiles> sampleFilesCollection;

    public SampleIds() {
    }

    public SampleIds(SampleIdsPK sampleIdsPK) {
        this.sampleIdsPK = sampleIdsPK;
    }

    public SampleIds(String id, String studyName) {
        this.sampleIdsPK = new SampleIdsPK(id, studyName);
    }

    public SampleIdsPK getSampleIdsPK() {
        return sampleIdsPK;
    }

    public void setSampleIdsPK(SampleIdsPK sampleIdsPK) {
        this.sampleIdsPK = sampleIdsPK;
    }

    @XmlTransient
    @JsonIgnore
    public Collection<SampleFiles> getSampleFilesCollection() {
        return sampleFilesCollection;
    }

    public void setSampleFilesCollection(Collection<SampleFiles> sampleFilesCollection) {
        this.sampleFilesCollection = sampleFilesCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (sampleIdsPK != null ? sampleIdsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SampleIds)) {
            return false;
        }
        SampleIds other = (SampleIds) object;
        if ((this.sampleIdsPK == null && other.sampleIdsPK != null) || (this.sampleIdsPK != null && !this.sampleIdsPK.equals(other.sampleIdsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.study.SampleIds[ sampleIdsPK=" + sampleIdsPK + " ]";
    }
    
}
