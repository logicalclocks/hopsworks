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
public class TrackStudyPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private int id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "dataset_id")
    private int datasetId;

    public TrackStudyPK() {
    }

    public TrackStudyPK(int id, int datasetId) {
        this.id = id;
        this.datasetId = datasetId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
        hash += (int) id;
        hash += (int) datasetId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TrackStudyPK)) {
            return false;
        }
        TrackStudyPK other = (TrackStudyPK) object;
        if (this.id != other.id) {
            return false;
        }
        if (this.datasetId != other.datasetId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.TrackStudyPK[ id=" + id + ", datasetId=" + datasetId + " ]";
    }
    
}
