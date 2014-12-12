/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Entity
@Table(name = "People")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "People.findAll", query = "SELECT p FROM People p"),
    @NamedQuery(name = "People.findByUid", query = "SELECT p FROM People p WHERE p.uid = :uid"),
    @NamedQuery(name = "People.findByUsername", query = "SELECT p FROM People p WHERE p.username = :username"),
    @NamedQuery(name = "People.findByPassword", query = "SELECT p FROM People p WHERE p.password = :password"),
    @NamedQuery(name = "People.findByEmail", query = "SELECT p FROM People p WHERE p.email = :email"),
    @NamedQuery(name = "People.findByFname", query = "SELECT p FROM People p WHERE p.fname = :fname"),
    @NamedQuery(name = "People.findByLname", query = "SELECT p FROM People p WHERE p.lname = :lname"),
    @NamedQuery(name = "People.findByActivated", query = "SELECT p FROM People p WHERE p.activated = :activated"),
    @NamedQuery(name = "People.findByHomeOrg", query = "SELECT p FROM People p WHERE p.homeOrg = :homeOrg"),
    @NamedQuery(name = "People.findByTitle", query = "SELECT p FROM People p WHERE p.title = :title"),
    @NamedQuery(name = "People.findByMobile", query = "SELECT p FROM People p WHERE p.mobile = :mobile"),
    @NamedQuery(name = "People.findByOrcid", query = "SELECT p FROM People p WHERE p.orcid = :orcid"),
    @NamedQuery(name = "People.findByFalseLogin", query = "SELECT p FROM People p WHERE p.falseLogin = :falseLogin"),
    @NamedQuery(name = "People.findByStatus", query = "SELECT p FROM People p WHERE p.status = :status"),
    @NamedQuery(name = "People.findByIsonline", query = "SELECT p FROM People p WHERE p.isonline = :isonline"),
    @NamedQuery(name = "People.findBySecret", query = "SELECT p FROM People p WHERE p.secret = :secret"),
    @NamedQuery(name = "People.findBySecurityQuestion", query = "SELECT p FROM People p WHERE p.securityQuestion = :securityQuestion"),
    @NamedQuery(name = "People.findBySecurityAnswer", query = "SELECT p FROM People p WHERE p.securityAnswer = :securityAnswer")})
public class People implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "uid")
    private Integer uid;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "username")
    private String username;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "password")
    private String password;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Size(max = 45)
    @Column(name = "email")
    private String email;
    @Size(max = 30)
    @Column(name = "fname")
    private String fname;
    @Size(max = 30)
    @Column(name = "lname")
    private String lname;
    @Basic(optional = false)
    @NotNull
    @Column(name = "activated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date activated;
    @Size(max = 100)
    @Column(name = "home_org")
    private String homeOrg;
    @Size(max = 10)
    @Column(name = "title")
    private String title;
    @Size(max = 20)
    @Column(name = "mobile")
    private String mobile;
    @Size(max = 20)
    @Column(name = "orcid")
    private String orcid;
    @Basic(optional = false)
    @NotNull
    @Column(name = "false_login")
    private int falseLogin;
    @Basic(optional = false)
    @NotNull
    @Column(name = "status")
    private int status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "isonline")
    private int isonline;
    @Size(max = 20)
    @Column(name = "secret")
    private String secret;
    @Size(max = 20)
    @Column(name = "security_question")
    private String securityQuestion;
    @Size(max = 128)
    @Column(name = "security_answer")
    private String securityAnswer;
    @OneToMany(mappedBy = "peopleuid")
    private Collection<Login> loginCollection;
    @OneToMany(mappedBy = "peopleuid")
    private Collection<Yubikey> yubikeyCollection;

    public People() {
    }

    public People(Integer uid) {
        this.uid = uid;
    }

    public People(Integer uid, String username, String password, Date activated, int falseLogin, int status, int isonline) {
        this.uid = uid;
        this.username = username;
        this.password = password;
        this.activated = activated;
        this.falseLogin = falseLogin;
        this.status = status;
        this.isonline = isonline;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public Date getActivated() {
        return activated;
    }

    public void setActivated(Date activated) {
        this.activated = activated;
    }

    public String getHomeOrg() {
        return homeOrg;
    }

    public void setHomeOrg(String homeOrg) {
        this.homeOrg = homeOrg;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public int getFalseLogin() {
        return falseLogin;
    }

    public void setFalseLogin(int falseLogin) {
        this.falseLogin = falseLogin;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getIsonline() {
        return isonline;
    }

    public void setIsonline(int isonline) {
        this.isonline = isonline;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    @XmlTransient
    @JsonIgnore
    public Collection<Login> getLoginCollection() {
        return loginCollection;
    }

    public void setLoginCollection(Collection<Login> loginCollection) {
        this.loginCollection = loginCollection;
    }

    @XmlTransient
    @JsonIgnore
    public Collection<Yubikey> getYubikeyCollection() {
        return yubikeyCollection;
    }

    public void setYubikeyCollection(Collection<Yubikey> yubikeyCollection) {
        this.yubikeyCollection = yubikeyCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (uid != null ? uid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof People)) {
            return false;
        }
        People other = (People) object;
        if ((this.uid == null && other.uid != null) || (this.uid != null && !this.uid.equals(other.uid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.security.ua.model.People[ uid=" + uid + " ]";
    }
    
}
