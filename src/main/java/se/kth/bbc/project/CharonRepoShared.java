/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.charon_repo_shared")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CharonRepoShared.findAll", query = "SELECT c FROM CharonRepoShared c"),
    @NamedQuery(name = "CharonRepoShared.findByProjectId", query = "SELECT c FROM CharonRepoShared c WHERE c.charonRepoSharedPK.projectId = :projectId"),
    @NamedQuery(name = "CharonRepoShared.findBySiteId", query = "SELECT c FROM CharonRepoShared c WHERE c.charonRepoSharedPK.siteId = :siteId"),
    @NamedQuery(name = "CharonRepoShared.findByPath", query = "SELECT c FROM CharonRepoShared c WHERE c.charonRepoSharedPK.path = :path"),
    @NamedQuery(name = "CharonRepoShared.findByPermissions", query = "SELECT c FROM CharonRepoShared c WHERE c.permissions = :permissions")})
public class CharonRepoShared implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CharonRepoSharedPK charonRepoSharedPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    private String permissions;
    @JoinColumn(name = "project_id", referencedColumnName = "id", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Project project;

    public CharonRepoShared() {
    }

    public CharonRepoShared(CharonRepoSharedPK charonRepoSharedPK) {
        this.charonRepoSharedPK = charonRepoSharedPK;
    }

    public CharonRepoShared(CharonRepoSharedPK charonRepoSharedPK, String permissions) {
        this.charonRepoSharedPK = charonRepoSharedPK;
        this.permissions = permissions;
    }

    public CharonRepoShared(int projectId, String siteId, String path) {
        this.charonRepoSharedPK = new CharonRepoSharedPK(projectId, siteId, path);
    }

    public CharonRepoSharedPK getCharonRepoSharedPK() {
        return charonRepoSharedPK;
    }

    public void setCharonRepoSharedPK(CharonRepoSharedPK charonRepoSharedPK) {
        this.charonRepoSharedPK = charonRepoSharedPK;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (charonRepoSharedPK != null ? charonRepoSharedPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CharonRepoShared)) {
            return false;
        }
        CharonRepoShared other = (CharonRepoShared) object;
        if ((this.charonRepoSharedPK == null && other.charonRepoSharedPK != null) || (this.charonRepoSharedPK != null && !this.charonRepoSharedPK.equals(other.charonRepoSharedPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.project.CharonRepoShared[ charonRepoSharedPK=" + charonRepoSharedPK + " ]";
    }
    
}
