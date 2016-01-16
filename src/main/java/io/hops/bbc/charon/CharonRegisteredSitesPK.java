/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc.charon;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Embeddable
public class CharonRegisteredSitesPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "project_id")
    private int projectId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "site_id")
    private int siteId;

    public CharonRegisteredSitesPK() {
    }

    public CharonRegisteredSitesPK(int projectId, int siteId) {
        this.projectId = projectId;
        this.siteId = siteId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) projectId;
        hash += (int) siteId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CharonRegisteredSitesPK)) {
            return false;
        }
        CharonRegisteredSitesPK other = (CharonRegisteredSitesPK) object;
        if (this.projectId != other.projectId) {
            return false;
        }
        if (this.siteId != other.siteId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.bbc.charon.tmp.CharonRegisteredSitesPK[ projectId=" + projectId + ", siteId=" + siteId + " ]";
    }

}
