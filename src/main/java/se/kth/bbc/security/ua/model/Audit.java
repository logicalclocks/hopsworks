/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "AUDIT")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Audit.findAll", query = "SELECT a FROM Audit a"),
    @NamedQuery(name = "Audit.findByUid", query = "SELECT a FROM Audit a WHERE a.uid = :uid"),
    @NamedQuery(name = "Audit.findByUsername", query = "SELECT a FROM Audit a WHERE a.username = :username"),
    @NamedQuery(name = "Audit.findByTime", query = "SELECT a FROM Audit a WHERE a.time = :time"),
    @NamedQuery(name = "Audit.findByRole", query = "SELECT a FROM Audit a WHERE a.role = :role"),
    @NamedQuery(name = "Audit.findByAction", query = "SELECT a FROM Audit a WHERE a.action = :action"),
    @NamedQuery(name = "Audit.findByResource", query = "SELECT a FROM Audit a WHERE a.resource = :resource"),
    @NamedQuery(name = "Audit.findByLogId", query = "SELECT a FROM Audit a WHERE a.logId = :logId")})
public class Audit implements Serializable {
    private static final long serialVersionUID = 1L;
    @Size(max = 8)
    @Column(name = "uid")
    private String uid;
    @Size(max = 80)
    @Column(name = "username")
    private String username;
    @Basic(optional = false)
    @NotNull
    @Column(name = "time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date time;
    @Size(max = 20)
    @Column(name = "role")
    private String role;
    @Size(max = 20)
    @Column(name = "action")
    private String action;
    @Size(max = 100)
    @Column(name = "resource")
    private String resource;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "log_id")
    private Long logId;

    public Audit() {
    }

    public Audit(Long logId) {
        this.logId = logId;
    }

    public Audit(Long logId, Date time) {
        this.logId = logId;
        this.time = time;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (logId != null ? logId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Audit)) {
            return false;
        }
        Audit other = (Audit) object;
        if ((this.logId == null && other.logId != null) || (this.logId != null && !this.logId.equals(other.logId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.security.ua.model.Audit[ logId=" + logId + " ]";
    }
    
}
