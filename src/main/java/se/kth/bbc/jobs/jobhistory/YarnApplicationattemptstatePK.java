/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author jdowling
 */
@Embeddable
public class YarnApplicationattemptstatePK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "applicationid")
    private String applicationid;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "applicationattemptid")
    private String applicationattemptid;

    public YarnApplicationattemptstatePK() {
    }

    public YarnApplicationattemptstatePK(String applicationid, String applicationattemptid) {
        this.applicationid = applicationid;
        this.applicationattemptid = applicationattemptid;
    }

    public String getApplicationid() {
        return applicationid;
    }

    public void setApplicationid(String applicationid) {
        this.applicationid = applicationid;
    }

    public String getApplicationattemptid() {
        return applicationattemptid;
    }

    public void setApplicationattemptid(String applicationattemptid) {
        this.applicationattemptid = applicationattemptid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (applicationid != null ? applicationid.hashCode() : 0);
        hash += (applicationattemptid != null ? applicationattemptid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof YarnApplicationattemptstatePK)) {
            return false;
        }
        YarnApplicationattemptstatePK other = (YarnApplicationattemptstatePK) object;
        if ((this.applicationid == null && other.applicationid != null) || (this.applicationid != null && !this.applicationid.equals(other.applicationid))) {
            return false;
        }
        if ((this.applicationattemptid == null && other.applicationattemptid != null) || (this.applicationattemptid != null && !this.applicationattemptid.equals(other.applicationattemptid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.jobs.yarn.YarnApplicationattemptstatePK[ applicationid=" + applicationid + ", applicationattemptid=" + applicationattemptid + " ]";
    }
    
}
