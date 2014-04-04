/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roshan
 */
@Entity
@Table(name = "study")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TrackStudy.findAll", query = "SELECT t FROM TrackStudy t"),
    @NamedQuery(name = "TrackStudy.findById", query = "SELECT t FROM TrackStudy t WHERE t.id = :id"),
    @NamedQuery(name = "TrackStudy.findByName", query = "SELECT t FROM TrackStudy t WHERE t.name = :name"),
    @NamedQuery(name = "TrackStudy.findByUsername", query = "SELECT t FROM TrackStudy t WHERE t.username = :username"),
    @NamedQuery(name = "TrackStudy.findAllStudy", query = "SELECT COUNT(t.name) FROM TrackStudy t"),
    @NamedQuery(name = "TrackStudy.findMembers", query = "SELECT DISTINCT COUNT(t.username) FROM TrackStudy t WHERE t.name = :name")})
    //@NamedQuery(name = "TrackStudy.findUsername", query = "SELECT * FROM TrackStudy t, Username u WHERE t.username = u.email AND t.username = :username")})

public class TrackStudy implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "username")
    private String username;

    public TrackStudy() {
    }

    public TrackStudy(Integer id) {
        this.id = id;
    }

    public TrackStudy(Integer id, String name, String username) {
        this.id = id;
        this.name = name;
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TrackStudy)) {
            return false;
        }
        TrackStudy other = (TrackStudy) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.kthfsdashboard.study.TrackStudy[ id=" + id + " ]";
    }
    
}
