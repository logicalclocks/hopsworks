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
@Table(name = "LOGIN")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Login.findAll", query = "SELECT l FROM Login l"),
    @NamedQuery(name = "Login.findByLoginId", query = "SELECT l FROM Login l WHERE l.loginId = :loginId"),
    @NamedQuery(name = "Login.findByUid", query = "SELECT l FROM Login l WHERE l.uid = :uid"),
    @NamedQuery(name = "Login.findByUsername", query = "SELECT l FROM Login l WHERE l.username = :username"),
    @NamedQuery(name = "Login.findByRole", query = "SELECT l FROM Login l WHERE l.role = :role"),
    @NamedQuery(name = "Login.findByTime", query = "SELECT l FROM Login l WHERE l.time = :time"),
    @NamedQuery(name = "Login.findByIp", query = "SELECT l FROM Login l WHERE l.ip = :ip"),
    @NamedQuery(name = "Login.findByOs", query = "SELECT l FROM Login l WHERE l.os = :os"),
    @NamedQuery(name = "Login.findByBrowser", query = "SELECT l FROM Login l WHERE l.browser = :browser"),
    @NamedQuery(name = "Login.findByAction", query = "SELECT l FROM Login l WHERE l.action = :action"),
    @NamedQuery(name = "Login.findByOutcome", query = "SELECT l FROM Login l WHERE l.outcome = :outcome")})
public class Login implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "login_id")
    private Long loginId;
    @Size(max = 8)
    @Column(name = "uid")
    private String uid;
    @Size(max = 80)
    @Column(name = "username")
    private String username;
    @Size(max = 20)
    @Column(name = "role")
    private String role;
    @Basic(optional = false)
    @NotNull
    @Column(name = "time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date time;
    @Size(max = 16)
    @Column(name = "ip")
    private String ip;
    @Size(max = 30)
    @Column(name = "os")
    private String os;
    @Size(max = 40)
    @Column(name = "browser")
    private String browser;
    @Size(max = 80)
    @Column(name = "action")
    private String action;
    @Size(max = 20)
    @Column(name = "outcome")
    private String outcome;

    public Login() {
    }

    public Login(Long loginId) {
        this.loginId = loginId;
    }

    public Login(Long loginId, Date time) {
        this.loginId = loginId;
        this.time = time;
    }

    public Long getLoginId() {
        return loginId;
    }

    public void setLoginId(Long loginId) {
        this.loginId = loginId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (loginId != null ? loginId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Login)) {
            return false;
        }
        Login other = (Login) object;
        if ((this.loginId == null && other.loginId != null) || (this.loginId != null && !this.loginId.equals(other.loginId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.security.ua.model.Login[ loginId=" + loginId + " ]";
    }
    
}
