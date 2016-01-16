/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.bbc.charon;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author jdowling
 */
@Entity
@Table(name = "charon_registered_sites")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CharonRegisteredSites.findAll", query = "SELECT c FROM CharonRegisteredSites c"),
    @NamedQuery(name = "CharonRegisteredSites.findByProjectId", query = "SELECT c FROM CharonRegisteredSites c WHERE c.charonRegisteredSitesPK.projectId = :projectId"),
    @NamedQuery(name = "CharonRegisteredSites.findBypProjectIdSiteId", query = "SELECT c FROM CharonRegisteredSites c WHERE c.charonRegisteredSitesPK.projectId = :projectId AND c.charonRegisteredSitesPK.siteId = :siteId "),
    @NamedQuery(name = "CharonRegisteredSites.findByEmail", query = "SELECT c FROM CharonRegisteredSites c WHERE c.email = :email"),
    @NamedQuery(name = "CharonRegisteredSites.findByName", query = "SELECT c FROM CharonRegisteredSites c WHERE c.name = :name"),
    @NamedQuery(name = "CharonRegisteredSites.findByAddr", query = "SELECT c FROM CharonRegisteredSites c WHERE c.addr = :addr")})
public class CharonRegisteredSites implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CharonRegisteredSitesPK charonRegisteredSitesPK;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "email")
    private String email;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "addr")
    private String addr;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "charonRegisteredSites")
    private Collection<CharonRepoShared> charonRepoSharedCollection;

    public CharonRegisteredSites() {
    }

    public CharonRegisteredSites(CharonRegisteredSitesPK charonRegisteredSitesPK, String email, String name, String addr) {
        this.charonRegisteredSitesPK = charonRegisteredSitesPK;
        this.email = email;
        this.name = name;
        this.addr = addr;
    }

    public CharonRegisteredSitesPK getCharonRegisteredSitesPK() {
        return charonRegisteredSitesPK;
    }

    public void setCharonRegisteredSitesPK(CharonRegisteredSitesPK charonRegisteredSitesPK) {
        this.charonRegisteredSitesPK = charonRegisteredSitesPK;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    @XmlTransient
    @JsonIgnore
    public Collection<CharonRepoShared> getCharonRepoSharedCollection() {
        return charonRepoSharedCollection;
    }

    public void setCharonRepoSharedCollection(Collection<CharonRepoShared> charonRepoSharedCollection) {
        this.charonRepoSharedCollection = charonRepoSharedCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (charonRegisteredSitesPK != null ? charonRegisteredSitesPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CharonRegisteredSites)) {
            return false;
        }
        CharonRegisteredSites other = (CharonRegisteredSites) object;
        if ((this.charonRegisteredSitesPK == null && other.charonRegisteredSitesPK != null) || (this.charonRegisteredSitesPK != null && !this.charonRegisteredSitesPK.equals(other.charonRegisteredSitesPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "io.hops.bbc.charon.tmp.CharonRegisteredSites[ charonRegisteredSitesPK=" + charonRegisteredSitesPK + " ]";
    }
    
}
