/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "yarn_applicationattemptstate", catalog = "hops", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "YarnApplicationattemptstate.findAll", query = "SELECT y FROM YarnApplicationattemptstate y"),
    @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationid", query = "SELECT y FROM YarnApplicationattemptstate y WHERE y.yarnApplicationattemptstatePK.applicationid = :applicationid"),
    @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationattemptid", query = "SELECT y FROM YarnApplicationattemptstate y WHERE y.yarnApplicationattemptstatePK.applicationattemptid = :applicationattemptid"),
    @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationattempthost", query = "SELECT y FROM YarnApplicationattemptstate y WHERE y.applicationattempthost = :applicationattempthost"),
    @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationattemptrpcport", query = "SELECT y FROM YarnApplicationattemptstate y WHERE y.applicationattemptrpcport = :applicationattemptrpcport"),
    @NamedQuery(name = "YarnApplicationattemptstate.findByApplicationattempttrakingurl", query = "SELECT y FROM YarnApplicationattemptstate y WHERE y.applicationattempttrakingurl = :applicationattempttrakingurl")})
public class YarnApplicationattemptstate implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected YarnApplicationattemptstatePK yarnApplicationattemptstatePK;
    @Lob
    @Column(name = "applicationattemptstate")
    private byte[] applicationattemptstate;
    @Size(max = 255)
    @Column(name = "applicationattempthost")
    private String applicationattempthost;
    @Column(name = "applicationattemptrpcport")
    private Integer applicationattemptrpcport;
    @Lob
    @Column(name = "applicationattempttokens")
    private byte[] applicationattempttokens;
    @Size(max = 120)
    @Column(name = "applicationattempttrakingurl")
    private String applicationattempttrakingurl;

    @JoinColumn(name = "applicationid", referencedColumnName = "applicationid", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private YarnApplicationstate yarnApplicationstate;

    public YarnApplicationattemptstate() {
    }

    public YarnApplicationattemptstate(YarnApplicationattemptstatePK yarnApplicationattemptstatePK) {
        this.yarnApplicationattemptstatePK = yarnApplicationattemptstatePK;
    }

    public YarnApplicationattemptstate(String applicationid, String applicationattemptid) {
        this.yarnApplicationattemptstatePK = new YarnApplicationattemptstatePK(applicationid, applicationattemptid);
    }

    public YarnApplicationattemptstatePK getYarnApplicationattemptstatePK() {
        return yarnApplicationattemptstatePK;
    }

    public void setYarnApplicationattemptstatePK(YarnApplicationattemptstatePK yarnApplicationattemptstatePK) {
        this.yarnApplicationattemptstatePK = yarnApplicationattemptstatePK;
    }

    public byte[] getApplicationattemptstate() {
        return applicationattemptstate;
    }

    public void setApplicationattemptstate(byte[] applicationattemptstate) {
        this.applicationattemptstate = applicationattemptstate;
    }

    public String getApplicationattempthost() {
        return applicationattempthost;
    }

    public void setApplicationattempthost(String applicationattempthost) {
        this.applicationattempthost = applicationattempthost;
    }

    public Integer getApplicationattemptrpcport() {
        return applicationattemptrpcport;
    }

    public void setApplicationattemptrpcport(Integer applicationattemptrpcport) {
        this.applicationattemptrpcport = applicationattemptrpcport;
    }

    public byte[] getApplicationattempttokens() {
        return applicationattempttokens;
    }

    public void setApplicationattempttokens(byte[] applicationattempttokens) {
        this.applicationattempttokens = applicationattempttokens;
    }

    public String getApplicationattempttrakingurl() {
        return applicationattempttrakingurl;
    }

    public void setApplicationattempttrakingurl(String applicationattempttrakingurl) {
        this.applicationattempttrakingurl = applicationattempttrakingurl;
    }

    public YarnApplicationstate getYarnApplicationstate() {
        return yarnApplicationstate;
    }

    public void setYarnApplicationstate(YarnApplicationstate yarnApplicationstate) {
        this.yarnApplicationstate = yarnApplicationstate;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (yarnApplicationattemptstatePK != null ? yarnApplicationattemptstatePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof YarnApplicationattemptstate)) {
            return false;
        }
        YarnApplicationattemptstate other = (YarnApplicationattemptstate) object;
        if ((this.yarnApplicationattemptstatePK == null && other.yarnApplicationattemptstatePK != null) || (this.yarnApplicationattemptstatePK != null && !this.yarnApplicationattemptstatePK.equals(other.yarnApplicationattemptstatePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.jobs.yarn.YarnApplicationattemptstate[ yarnApplicationattemptstatePK=" + yarnApplicationattemptstatePK + " ]";
    }
    
}
