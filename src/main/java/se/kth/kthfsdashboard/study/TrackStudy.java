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
@Table(name = "study")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TrackStudy.findAll", query = "SELECT t FROM TrackStudy t"),
    @NamedQuery(name = "TrackStudy.findById", query = "SELECT t FROM TrackStudy t WHERE t.trackStudyPK.id = :id"),
    @NamedQuery(name = "TrackStudy.findByDatasetId", query = "SELECT t FROM TrackStudy t WHERE t.trackStudyPK.datasetId = :datasetId"),
    @NamedQuery(name = "TrackStudy.findByName", query = "SELECT t FROM TrackStudy t WHERE t.name = :name")})
public class TrackStudy implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TrackStudyPK trackStudyPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "name")
    private String name;

    public TrackStudy() {
    }

    public TrackStudy(TrackStudyPK trackStudyPK) {
        this.trackStudyPK = trackStudyPK;
    }

    public TrackStudy(TrackStudyPK trackStudyPK, String name) {
        this.trackStudyPK = trackStudyPK;
        this.name = name;
    }

    public TrackStudy(int id, int datasetId) {
        this.trackStudyPK = new TrackStudyPK(id, datasetId);
    }

    public TrackStudyPK getTrackStudyPK() {
        return trackStudyPK;
    }

    public void setTrackStudyPK(TrackStudyPK trackStudyPK) {
        this.trackStudyPK = trackStudyPK;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (trackStudyPK != null ? trackStudyPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TrackStudy)) {
            return false;
        }
        TrackStudy other = (TrackStudy) object;
        if ((this.trackStudyPK == null && other.trackStudyPK != null) || (this.trackStudyPK != null && !this.trackStudyPK.equals(other.trackStudyPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.TrackStudy[ trackStudyPK=" + trackStudyPK + " ]";
    }
    
}
