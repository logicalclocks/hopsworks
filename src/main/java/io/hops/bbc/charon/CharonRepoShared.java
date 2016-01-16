/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc.charon;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "charon_repo_shared")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CharonRepoShared.findAll", query = "SELECT c FROM CharonRepoShared c"),
    @NamedQuery(name = "CharonRepoShared.findByProjectId", query = "SELECT c FROM CharonRepoShared c WHERE c.charonRepoSharedPK.projectId = :projectId"),
    @NamedQuery(name = "CharonRepoShared.findBySiteId", query = "SELECT c FROM CharonRepoShared c WHERE c.charonRepoSharedPK.siteId = :siteId"),
    @NamedQuery(name = "CharonRepoShared.findByPath", query = "SELECT c FROM CharonRepoShared c WHERE c.charonRepoSharedPK.path = :path"),
    @NamedQuery(name = "CharonRepoShared.findByToken", query = "SELECT c FROM CharonRepoShared c WHERE c.token = :token"),
    @NamedQuery(name = "CharonRepoShared.findByPermissions", query = "SELECT c FROM CharonRepoShared c WHERE c.permissions = :permissions")})
public class CharonRepoShared implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CharonRepoSharedPK charonRepoSharedPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "token")
    private String token;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "permissions")
    private String permissions;
    @JoinColumns({
        @JoinColumn(name = "project_id", referencedColumnName = "project_id", insertable = false, updatable = false),
        @JoinColumn(name = "site_id", referencedColumnName = "site_id", insertable = false, updatable = false)})
    @ManyToOne(optional = false)
    private CharonRegisteredSites charonRegisteredSites;

    public CharonRepoShared() {
    }

    public CharonRepoShared(CharonRepoSharedPK charonRepoSharedPK, String token, 
            String permissions) {
        this.charonRepoSharedPK = charonRepoSharedPK;
        this.token = token;
        this.permissions = permissions;
    }


    public CharonRepoSharedPK getCharonRepoSharedPK() {
        return charonRepoSharedPK;
    }

    public void setCharonRepoSharedPK(CharonRepoSharedPK charonRepoSharedPK) {
        this.charonRepoSharedPK = charonRepoSharedPK;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public CharonRegisteredSites getCharonRegisteredSites() {
        return charonRegisteredSites;
    }

    public void setCharonRegisteredSites(CharonRegisteredSites charonRegisteredSites) {
        this.charonRegisteredSites = charonRegisteredSites;
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
        return "io.hops.bbc.charon.tmp.CharonRepoShared[ charonRepoSharedPK=" + charonRepoSharedPK + " ]";
    }
    
}
