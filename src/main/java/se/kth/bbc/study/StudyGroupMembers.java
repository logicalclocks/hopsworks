/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "study_group_members")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "StudyGroupMembers.findAll", query = "SELECT s FROM StudyGroupMembers s"),
    @NamedQuery(name = "StudyGroupMembers.findByStudyname", query = "SELECT s FROM StudyGroupMembers s WHERE s.studyGroupMembersPK.studyname = :studyname"),
    @NamedQuery(name = "StudyGroupMembers.findByUsername", query = "SELECT s FROM StudyGroupMembers s WHERE s.studyGroupMembersPK.username = :username"),
    @NamedQuery(name = "StudyGroupMembers.findByTimeadded", query = "SELECT s FROM StudyGroupMembers s WHERE s.timeadded = :timeadded"),
    @NamedQuery(name = "StudyGroupMembers.findByAddedBy", query = "SELECT s FROM StudyGroupMembers s WHERE s.addedBy = :addedBy")})
public class StudyGroupMembers implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected StudyGroupMembersPK studyGroupMembersPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "timeadded")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timeadded;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "added_by")
    private String addedBy;

    public StudyGroupMembers() {
    }

    public StudyGroupMembers(StudyGroupMembersPK studyGroupMembersPK) {
        this.studyGroupMembersPK = studyGroupMembersPK;
    }

    public StudyGroupMembers(StudyGroupMembersPK studyGroupMembersPK, Date timeadded, String addedBy) {
        this.studyGroupMembersPK = studyGroupMembersPK;
        this.timeadded = timeadded;
        this.addedBy = addedBy;
    }

    public StudyGroupMembers(String studyname, String username) {
        this.studyGroupMembersPK = new StudyGroupMembersPK(studyname, username);
    }

    public StudyGroupMembersPK getStudyGroupMembersPK() {
        return studyGroupMembersPK;
    }

    public void setStudyGroupMembersPK(StudyGroupMembersPK studyGroupMembersPK) {
        this.studyGroupMembersPK = studyGroupMembersPK;
    }

    public Date getTimeadded() {
        return timeadded;
    }

    public void setTimeadded(Date timeadded) {
        this.timeadded = timeadded;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (studyGroupMembersPK != null ? studyGroupMembersPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StudyGroupMembers)) {
            return false;
        }
        StudyGroupMembers other = (StudyGroupMembers) object;
        if ((this.studyGroupMembersPK == null && other.studyGroupMembersPK != null) || (this.studyGroupMembersPK != null && !this.studyGroupMembersPK.equals(other.studyGroupMembersPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.StudyGroupMembers[ studyGroupMembersPK=" + studyGroupMembersPK + " ]";
    }
    
}
