/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "People_Group")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PeopleGroup.findAll", query = "SELECT p FROM PeopleGroup p"),
    @NamedQuery(name = "PeopleGroup.findByUid", query = "SELECT p FROM PeopleGroup p WHERE p.uid = :uid"),
    @NamedQuery(name = "PeopleGroup.findByPgid", query = "SELECT p FROM PeopleGroup p WHERE p.pgid = :pgid"),
    @NamedQuery(name = "PeopleGroup.findByGid", query = "SELECT p FROM PeopleGroup p WHERE p.gid = :gid")})
public class PeopleGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name = "uid")
    private Integer uid;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Pgid")
    private Integer pgid;
    @Column(name = "gid")
    private Integer gid;

    public PeopleGroup() {
    }

    public PeopleGroup(Integer pgid) {
        this.pgid = pgid;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getPgid() {
        return pgid;
    }

    public void setPgid(Integer pgid) {
        this.pgid = pgid;
    }

    public Integer getGid() {
        return gid;
    }

    public void setGid(Integer gid) {
        this.gid = gid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (pgid != null ? pgid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PeopleGroup)) {
            return false;
        }
        PeopleGroup other = (PeopleGroup) object;
        if ((this.pgid == null && other.pgid != null) || (this.pgid != null && !this.pgid.equals(other.pgid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.security.ua.model.PeopleGroup[ pgid=" + pgid + " ]";
    }
    
}
