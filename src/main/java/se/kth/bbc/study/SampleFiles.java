/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "SampleFiles")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SampleFiles.findAll", query = "SELECT s FROM SampleFiles s"),
    @NamedQuery(name = "SampleFiles.findById", query = "SELECT s FROM SampleFiles s WHERE s.sampleFilesPK.id = :id"),
    @NamedQuery(name = "SampleFiles.findByFileType", query = "SELECT s FROM SampleFiles s WHERE s.fileType = :fileType"),
    @NamedQuery(name = "SampleFiles.findByFilename", query = "SELECT s FROM SampleFiles s WHERE s.sampleFilesPK.filename = :filename"),
    @NamedQuery(name = "SampleFiles.findByStatus", query = "SELECT s FROM SampleFiles s WHERE s.status = :status")})
public class SampleFiles implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SampleFilesPK sampleFilesPK;
    @Size(max = 6)
    @Column(name = "file_type")
    private String fileType;
    @Size(max = 15)
    @Column(name = "status")
    private String status;
    //@JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    @JoinColumns({
        @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false),
        @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)})
    @ManyToOne(optional = false)
    private SampleIds sampleIds;

    public SampleFiles() {
    }

    public SampleFiles(SampleFilesPK sampleFilesPK) {
        this.sampleFilesPK = sampleFilesPK;
    }

    public SampleFiles(String id, String filename) {
        this.sampleFilesPK = new SampleFilesPK(id, filename);
    }

    public SampleFilesPK getSampleFilesPK() {
        return sampleFilesPK;
    }

    public void setSampleFilesPK(SampleFilesPK sampleFilesPK) {
        this.sampleFilesPK = sampleFilesPK;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SampleIds getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(SampleIds sampleIds) {
        this.sampleIds = sampleIds;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (sampleFilesPK != null ? sampleFilesPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SampleFiles)) {
            return false;
        }
        SampleFiles other = (SampleFiles) object;
        if ((this.sampleFilesPK == null && other.sampleFilesPK != null) || (this.sampleFilesPK != null && !this.sampleFilesPK.equals(other.sampleFilesPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.study.SampleFiles[ sampleFilesPK=" + sampleFilesPK + " ]";
    }
    
}
