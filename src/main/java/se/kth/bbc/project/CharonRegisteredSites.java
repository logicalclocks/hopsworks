/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.project;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.charon_registered_sites")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CharonRegisteredSites.findAll", query = "SELECT c FROM CharonRegisteredSites c"),
    @NamedQuery(name = "CharonRegisteredSites.findByProjectId", query = "SELECT c FROM CharonRegisteredSites c WHERE c.charonRegisteredSitesPK.projectId = :projectId"),
    @NamedQuery(name = "CharonRegisteredSites.findBySiteId", query = "SELECT c FROM CharonRegisteredSites c WHERE c.charonRegisteredSitesPK.siteId = :siteId")})
public class CharonRegisteredSites implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CharonRegisteredSitesPK charonRegisteredSitesPK;
    @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Project project;

    public CharonRegisteredSites() {
    }

    public CharonRegisteredSites(CharonRegisteredSitesPK charonRegisteredSitesPK) {
        this.charonRegisteredSitesPK = charonRegisteredSitesPK;
    }

    public CharonRegisteredSites(int projectId, String siteId) {
        this.charonRegisteredSitesPK = new CharonRegisteredSitesPK(projectId, siteId);
    }

    public CharonRegisteredSitesPK getCharonRegisteredSitesPK() {
        return charonRegisteredSitesPK;
    }

    public void setCharonRegisteredSitesPK(CharonRegisteredSitesPK charonRegisteredSitesPK) {
        this.charonRegisteredSitesPK = charonRegisteredSitesPK;
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
        return "se.kth.bbc.project.CharonRegisteredSites[ charonRegisteredSitesPK=" + charonRegisteredSitesPK + " ]";
    }
    
}
