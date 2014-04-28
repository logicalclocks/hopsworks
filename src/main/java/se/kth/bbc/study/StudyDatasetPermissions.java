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
@Table(name = "study_dataset_permissions")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "StudyDatasetPermissions.findAll", query = "SELECT s FROM StudyDatasetPermissions s"),
    @NamedQuery(name = "StudyDatasetPermissions.findByStudyId", query = "SELECT s FROM StudyDatasetPermissions s WHERE s.studyDatasetPermissionsPK.studyId = :studyId"),
    @NamedQuery(name = "StudyDatasetPermissions.findByDatasetId", query = "SELECT s FROM StudyDatasetPermissions s WHERE s.studyDatasetPermissionsPK.datasetId = :datasetId")})
public class StudyDatasetPermissions implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected StudyDatasetPermissionsPK studyDatasetPermissionsPK;

    public StudyDatasetPermissions() {
    }

    public StudyDatasetPermissions(StudyDatasetPermissionsPK studyDatasetPermissionsPK) {
        this.studyDatasetPermissionsPK = studyDatasetPermissionsPK;
    }

    public StudyDatasetPermissions(int studyId, int datasetId) {
        this.studyDatasetPermissionsPK = new StudyDatasetPermissionsPK(studyId, datasetId);
    }

    public StudyDatasetPermissionsPK getStudyDatasetPermissionsPK() {
        return studyDatasetPermissionsPK;
    }

    public void setStudyDatasetPermissionsPK(StudyDatasetPermissionsPK studyDatasetPermissionsPK) {
        this.studyDatasetPermissionsPK = studyDatasetPermissionsPK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (studyDatasetPermissionsPK != null ? studyDatasetPermissionsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StudyDatasetPermissions)) {
            return false;
        }
        StudyDatasetPermissions other = (StudyDatasetPermissions) object;
        if ((this.studyDatasetPermissionsPK == null && other.studyDatasetPermissionsPK != null) || (this.studyDatasetPermissionsPK != null && !this.studyDatasetPermissionsPK.equals(other.studyDatasetPermissionsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.StudyDatasetPermissions[ studyDatasetPermissionsPK=" + studyDatasetPermissionsPK + " ]";
    }
    
}
