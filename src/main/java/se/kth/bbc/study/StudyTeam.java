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
@Table(name = "StudyTeam")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "StudyTeam.findAll", query = "SELECT s FROM StudyTeam s"),
    @NamedQuery(name = "StudyTeam.findByName", query = "SELECT s FROM StudyTeam s WHERE s.studyTeamPK.name = :name"),
    @NamedQuery(name = "StudyTeam.findByTeamMember", query = "SELECT s FROM StudyTeam s WHERE s.studyTeamPK.teamMember = :teamMember AND s.teamRole != :teamRole"),
    @NamedQuery(name = "StudyTeam.findByTeamRole", query = "SELECT s FROM StudyTeam s WHERE s.teamRole = :teamRole"),
    @NamedQuery(name = "StudyTeam.findByTimestamp", query = "SELECT s FROM StudyTeam s WHERE s.timestamp = :timestamp"),
    @NamedQuery(name = "StudyTeam.countMastersByStudy", query = "SELECT COUNT(DISTINCT s.studyTeamPK.teamMember) FROM StudyTeam s WHERE s.studyTeamPK.name=:name AND s.teamRole = :teamRole"),
    @NamedQuery(name = "StudyTeam.countAllMembers", query = "SELECT s.studyTeamPK.teamMember FROM StudyTeam s WHERE s.studyTeamPK.name = :name"),
    @NamedQuery(name = "StudyTeam.findMembersByRole", query = "SELECT s FROM StudyTeam s WHERE s.studyTeamPK.name = :name AND s.teamRole = :teamRole"),
    @NamedQuery(name = "StudyTeam.findMembersByName", query = "SELECT s FROM StudyTeam s WHERE s.studyTeamPK.name = :name"),
    @NamedQuery(name = "StudyTeam.findByNameAndTeamMember", query = "SELECT s FROM StudyTeam s WHERE s.studyTeamPK.name = :name AND s.studyTeamPK.teamMember = :teamMember")
})

public class StudyTeam implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected StudyTeamPK studyTeamPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "team_role")
    private String teamRole;
    @Basic(optional = false)
    @NotNull
    @Column(name = "timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public StudyTeam() {
    }

    public StudyTeam(StudyTeamPK studyTeamPK) {
        this.studyTeamPK = studyTeamPK;
    }

    public StudyTeam(StudyTeamPK studyTeamPK, String teamRole, Date timestamp) {
        this.studyTeamPK = studyTeamPK;
        this.teamRole = teamRole;
        this.timestamp = timestamp;
    }

    public StudyTeam(String name, String teamMember) {
        this.studyTeamPK = new StudyTeamPK(name, teamMember);
    }

    public StudyTeamPK getStudyTeamPK() {
        return studyTeamPK;
    }

    public void setStudyTeamPK(StudyTeamPK studyTeamPK) {
        this.studyTeamPK = studyTeamPK;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (studyTeamPK != null ? studyTeamPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StudyTeam)) {
            return false;
        }
        StudyTeam other = (StudyTeam) object;
        if ((this.studyTeamPK == null && other.studyTeamPK != null) || (this.studyTeamPK != null && !this.studyTeamPK.equals(other.studyTeamPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.study.StudyTeam[ studyTeamPK=" + studyTeamPK + " ]";
    }
    
}
