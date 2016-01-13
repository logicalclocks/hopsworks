/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class CharonRepoSharedPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "project_id")
    private int projectId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "site_id")
    private String siteId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    private String path;

    public CharonRepoSharedPK() {
    }

    public CharonRepoSharedPK(int projectId, String siteId, String path) {
        this.projectId = projectId;
        this.siteId = siteId;
        this.path = path;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) projectId;
        hash += (siteId != null ? siteId.hashCode() : 0);
        hash += (path != null ? path.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CharonRepoSharedPK)) {
            return false;
        }
        CharonRepoSharedPK other = (CharonRepoSharedPK) object;
        if (this.projectId != other.projectId) {
            return false;
        }
        if ((this.siteId == null && other.siteId != null) || (this.siteId != null && !this.siteId.equals(other.siteId))) {
            return false;
        }
        if ((this.path == null && other.path != null) || (this.path != null && !this.path.equals(other.path))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.project.CharonRepoSharedPK[ projectId=" + projectId + ", siteId=" + siteId + ", path=" + path + " ]";
    }
    
}
