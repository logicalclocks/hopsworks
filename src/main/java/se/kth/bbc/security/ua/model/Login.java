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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author gholami
 */
@Entity
@Table(name = "Login")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Login.findAll", query = "SELECT l FROM Login l"),
    @NamedQuery(name = "Login.findByLoginid", query = "SELECT l FROM Login l WHERE l.loginid = :loginid"),
    @NamedQuery(name = "Login.findByLastLogin", query = "SELECT l FROM Login l WHERE l.lastLogin = :lastLogin"),
    @NamedQuery(name = "Login.findByLastIp", query = "SELECT l FROM Login l WHERE l.lastIp = :lastIp"),
    @NamedQuery(name = "Login.findByOsPlatform", query = "SELECT l FROM Login l WHERE l.osPlatform = :osPlatform"),
    @NamedQuery(name = "Login.findByLogout", query = "SELECT l FROM Login l WHERE l.logout = :logout")})
public class Login implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "loginid")
    private Long loginid;
    @Column(name = "last_login")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;
    @Size(max = 45)
    @Column(name = "last_ip")
    private String lastIp;
    @Column(name = "os_platform")
    private Integer osPlatform;
    @Column(name = "logout")
    @Temporal(TemporalType.TIMESTAMP)
    private Date logout;
    @JoinColumn(name = "People_uid", referencedColumnName = "uid")
    @ManyToOne
    private People peopleuid;

    public Login() {
    }

    public Login(Long loginid) {
        this.loginid = loginid;
    }

    public Long getLoginid() {
        return loginid;
    }

    public void setLoginid(Long loginid) {
        this.loginid = loginid;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public Integer getOsPlatform() {
        return osPlatform;
    }

    public void setOsPlatform(Integer osPlatform) {
        this.osPlatform = osPlatform;
    }

    public Date getLogout() {
        return logout;
    }

    public void setLogout(Date logout) {
        this.logout = logout;
    }

    public People getPeopleuid() {
        return peopleuid;
    }

    public void setPeopleuid(People peopleuid) {
        this.peopleuid = peopleuid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (loginid != null ? loginid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Login)) {
            return false;
        }
        Login other = (Login) object;
        if ((this.loginid == null && other.loginid != null) || (this.loginid != null && !this.loginid.equals(other.loginid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.security.ua.model.Login[ loginid=" + loginid + " ]";
    }
    
}
