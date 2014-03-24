/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "dataset_requests")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "DatasetRequests.findAll", query = "SELECT d FROM DatasetRequests d"),
    @NamedQuery(name = "DatasetRequests.findByStudyId", query = "SELECT d FROM DatasetRequests d WHERE d.datasetRequestsPK.studyId = :studyId"),
    @NamedQuery(name = "DatasetRequests.findByDatasetId", query = "SELECT d FROM DatasetRequests d WHERE d.datasetRequestsPK.datasetId = :datasetId"),
    @NamedQuery(name = "DatasetRequests.findByReason", query = "SELECT d FROM DatasetRequests d WHERE d.reason = :reason")})
public class DatasetRequests implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected DatasetRequestsPK datasetRequestsPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 512)
    @Column(name = "reason")
    private String reason;

    public DatasetRequests() {
    }

    public DatasetRequests(DatasetRequestsPK datasetRequestsPK) {
        this.datasetRequestsPK = datasetRequestsPK;
    }

    public DatasetRequests(DatasetRequestsPK datasetRequestsPK, String reason) {
        this.datasetRequestsPK = datasetRequestsPK;
        this.reason = reason;
    }

    public DatasetRequests(int studyId, int datasetId) {
        this.datasetRequestsPK = new DatasetRequestsPK(studyId, datasetId);
    }

    public DatasetRequestsPK getDatasetRequestsPK() {
        return datasetRequestsPK;
    }

    public void setDatasetRequestsPK(DatasetRequestsPK datasetRequestsPK) {
        this.datasetRequestsPK = datasetRequestsPK;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (datasetRequestsPK != null ? datasetRequestsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetRequests)) {
            return false;
        }
        DatasetRequests other = (DatasetRequests) object;
        if ((this.datasetRequestsPK == null && other.datasetRequestsPK != null) || (this.datasetRequestsPK != null && !this.datasetRequestsPK.equals(other.datasetRequestsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.DatasetRequests[ datasetRequestsPK=" + datasetRequestsPK + " ]";
    }
    
}
