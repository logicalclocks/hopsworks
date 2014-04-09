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
import javax.validation.constraints.Size;

/**
 *
 * @author roshan
 */
@Embeddable

public class StudyGroupMembersPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "studyname")
    private String studyname;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "username")
    private String username;
    

    public StudyGroupMembersPK() {
    }

    public StudyGroupMembersPK(String studyname, String username) {
        this.studyname = studyname;
        this.username = username;
    }

    public String getStudyname() {
        return studyname;
    }

    public void setStudyname(String studyname) {
        this.studyname = studyname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (studyname != null ? studyname.hashCode() : 0);
        hash += (username != null ? username.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StudyGroupMembersPK)) {
            return false;
        }
        StudyGroupMembersPK other = (StudyGroupMembersPK) object;
        if ((this.studyname == null && other.studyname != null) || (this.studyname != null && !this.studyname.equals(other.studyname))) {
            return false;
        }
        if ((this.username == null && other.username != null) || (this.username != null && !this.username.equals(other.username))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.StudyGroupMembersPK[ studyname=" + studyname + ", username=" + username + " ]";
    }

    
}
