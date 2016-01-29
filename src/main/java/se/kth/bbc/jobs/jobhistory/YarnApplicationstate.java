/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hops.yarn_applicationstate")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "YarnApplicationstate.findAll", query = "SELECT y FROM YarnApplicationstate y"),
    @NamedQuery(name = "YarnApplicationstate.findByApplicationid", query = "SELECT y FROM YarnApplicationstate y WHERE y.applicationid = :applicationid"),
    @NamedQuery(name = "YarnApplicationstate.findByAppuser", query = "SELECT y FROM YarnApplicationstate y WHERE y.appuser = :appuser"),
    @NamedQuery(name = "YarnApplicationstate.findByAppname", query = "SELECT y FROM YarnApplicationstate y WHERE y.appname = :appname ORDER BY y.applicationid DESC"),
    @NamedQuery(name = "YarnApplicationstate.findByAppsmstate", query = "SELECT y FROM YarnApplicationstate y WHERE y.appsmstate = :appsmstate")})
public class YarnApplicationstate implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "applicationid")
    private String applicationid;
    @Lob
    @Column(name = "appstate")
    private byte[] appstate;
    @Size(max = 45)
    @Column(name = "appuser")
    private String appuser;
    @Size(max = 200)
    @Column(name = "appname")
    private String appname;
    @Size(max = 45)
    @Column(name = "appsmstate")
    private String appsmstate;

    public YarnApplicationstate() {
    }

    public YarnApplicationstate(String applicationid) {
        this.applicationid = applicationid;
    }

    public String getApplicationid() {
        return applicationid;
    }

    public void setApplicationid(String applicationid) {
        this.applicationid = applicationid;
    }

    public byte[] getAppstate() {
        return appstate;
    }

    public void setAppstate(byte[] appstate) {
        this.appstate = appstate;
    }

    public String getAppuser() {
        return appuser;
    }

    public void setAppuser(String appuser) {
        this.appuser = appuser;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getAppsmstate() {
        return appsmstate;
    }

    public void setAppsmstate(String appsmstate) {
        this.appsmstate = appsmstate;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (applicationid != null ? applicationid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof YarnApplicationstate)) {
            return false;
        }
        YarnApplicationstate other = (YarnApplicationstate) object;
        if ((this.applicationid == null && other.applicationid != null) || (this.applicationid != null && !this.applicationid.equals(other.applicationid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.jobs.jobhistory.YarnApplicationstate[ applicationid=" + applicationid + " ]";
    }
    
}
