package se.kth.hopsworks.user.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.bbc.security.ua.SecurityQuestion;
import se.kth.bbc.security.ua.model.User;

@Entity
@Table(name = "hopsworks.users")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Users.findAll",
          query = "SELECT u FROM Users u"),
  @NamedQuery(name = "Users.findByUid",
          query = "SELECT u FROM Users u WHERE u.uid = :uid"),
  @NamedQuery(name = "Users.findByUsername",
          query = "SELECT u FROM Users u WHERE u.username = :username"),
  @NamedQuery(name = "Users.findByPassword",
          query = "SELECT u FROM Users u WHERE u.password = :password"),
  @NamedQuery(name = "Users.findByEmail",
          query = "SELECT u FROM Users u WHERE u.email = :email"),
  @NamedQuery(name = "Users.findByFname",
          query = "SELECT u FROM Users u WHERE u.fname = :fname"),
  @NamedQuery(name = "Users.findByLname",
          query = "SELECT u FROM Users u WHERE u.lname = :lname"),
  @NamedQuery(name = "Users.findByActivated",
          query = "SELECT u FROM Users u WHERE u.activated = :activated"),
  @NamedQuery(name = "Users.findByTitle",
          query = "SELECT u FROM Users u WHERE u.title = :title"),
  @NamedQuery(name = "Users.findByOrcid",
          query = "SELECT u FROM Users u WHERE u.orcid = :orcid"),
  @NamedQuery(name = "Users.findByFalseLogin",
          query = "SELECT u FROM Users u WHERE u.falseLogin = :falseLogin"),
  @NamedQuery(name = "Users.findByIsonline",
          query = "SELECT u FROM Users u WHERE u.isonline = :isonline"),
  @NamedQuery(name = "Users.findBySecret",
          query = "SELECT u FROM Users u WHERE u.secret = :secret"),
  @NamedQuery(name = "Users.findByValidationKey",
          query = "SELECT u FROM Users u WHERE u.validationKey = :validationKey"),
  @NamedQuery(name = "Users.findBySecurityQuestion",
          query
          = "SELECT u FROM Users u WHERE u.securityQuestion = :securityQuestion"),
  @NamedQuery(name = "Users.findBySecurityAnswer",
          query
          = "SELECT u FROM Users u WHERE u.securityAnswer = :securityAnswer"),
  @NamedQuery(name = "Users.findByYubikeyUser",
          query = "SELECT u FROM Users u WHERE u.yubikeyUser = :yubikeyUser"),
  @NamedQuery(name = "Users.findByPasswordChanged",
          query
          = "SELECT u FROM Users u WHERE u.passwordChanged = :passwordChanged"),
  @NamedQuery(name = "Users.findByNotes",
          query = "SELECT u FROM Users u WHERE u.notes = :notes"),
  @NamedQuery(name = "Users.findByMobile",
          query = "SELECT u FROM Users u WHERE u.mobile = :mobile"),
  @NamedQuery(name = "Users.findByStatus",
          query = "SELECT u FROM Users u WHERE u.status = :status")})
public class Users implements Serializable {
  /*
   * offline: -1
   * online: 1
   */

  public static final int IS_ONLINE = 1;
  public static final int IS_OFFLINE = -1;

  public static final int ALLOWED_FALSE_LOGINS = 5;
  //hopsworks user prefix username prefix
//  public static final String USERNAME_PREFIX = "meb";

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "uid")
  private Integer uid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 10)
  @Column(name = "username")
  private String username;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "password")
  private String password;
  // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
  @Size(max = 254)
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
  @Size(max = 10)
  @Column(name = "title")
  private String title;
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
  @Size(max = 128)
  @Column(name = "validation_key")
  private String validationKey;
  @Size(max = 20)
  @Enumerated(EnumType.STRING)
  @Column(name = "security_question")
  private SecurityQuestion securityQuestion;
  @Size(max = 128)
  @Column(name = "security_answer")
  private String securityAnswer;
  @Basic(optional = false)
  @NotNull
  @Column(name = "yubikey_user")
  private int yubikeyUser;
  @Basic(optional = false)
  @NotNull
  @Column(name = "password_changed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date passwordChanged;
  @Size(max = 500)
  @Column(name = "notes")
  private String notes;
  @Size(max = 15)
  @Column(name = "mobile")
  private String mobile;
  @JoinTable(name = "hopsworks.people_group",
          joinColumns = {
            @JoinColumn(name = "uid",
                    referencedColumnName = "uid")},
          inverseJoinColumns = {
            @JoinColumn(name = "gid",
                    referencedColumnName = "gid")})
  @ManyToMany
  private Collection<BbcGroup> bbcGroupCollection;

  public Users() {
  }

  public Users(Integer uid) {
    this.uid = uid;
  }

  public Users(Integer uid, String username, String password, Date activated,
          int falseLogin, int isonline, int yubikeyUser,
          Date passwordChanged, int status) {
    this.uid = uid;
    this.username = username;
    this.password = password;
    this.activated = activated;
    this.falseLogin = falseLogin;
    this.isonline = isonline;
    this.yubikeyUser = yubikeyUser;
    this.passwordChanged = passwordChanged;
    this.status = status;
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

  @XmlTransient
  @JsonIgnore
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

  @XmlTransient
  @JsonIgnore
  public Date getActivated() {
    return activated;
  }

  public void setActivated(Date activated) {
    this.activated = activated;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @XmlTransient
  @JsonIgnore
  public String getOrcid() {
    return orcid;
  }

  public void setOrcid(String orcid) {
    this.orcid = orcid;
  }

  @XmlTransient
  @JsonIgnore
  public int getFalseLogin() {
    return falseLogin;
  }

  public void setFalseLogin(int falseLogin) {
    this.falseLogin = falseLogin;
  }

  public int getIsonline() {
    return isonline;
  }

  public void setIsonline(int isonline) {
    this.isonline = isonline;
  }

  @XmlTransient
  @JsonIgnore
  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  @XmlTransient
  @JsonIgnore
  public String getValidationKey() {
    return validationKey;
  }

  public void setValidationKey(String validationKey) {
    this.validationKey = validationKey;
  }

  @XmlTransient
  @JsonIgnore
  public SecurityQuestion getSecurityQuestion() {
    return securityQuestion;
  }

  public void setSecurityQuestion(SecurityQuestion securityQuestion) {
    this.securityQuestion = securityQuestion;
  }

  @XmlTransient
  @JsonIgnore
  public String getSecurityAnswer() {
    return securityAnswer;
  }

  public void setSecurityAnswer(String securityAnswer) {
    this.securityAnswer = securityAnswer;
  }

  @XmlTransient
  @JsonIgnore
  public int getYubikeyUser() {
    return yubikeyUser;
  }

  public void setYubikeyUser(int yubikeyUser) {
    this.yubikeyUser = yubikeyUser;
  }

  public Date getPasswordChanged() {
    return passwordChanged;
  }

  public void setPasswordChanged(Date passwordChanged) {
    this.passwordChanged = passwordChanged;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  @XmlTransient
  @JsonIgnore
  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<BbcGroup> getBbcGroupCollection() {
    return bbcGroupCollection;
  }

  public void setBbcGroupCollection(Collection<BbcGroup> bbcGroupCollection) {
    this.bbcGroupCollection = bbcGroupCollection;
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
    if (!(object instanceof Users)) {
      return false;
    }
    Users other = (Users) object;
    if ((this.uid == null && other.uid != null) || (this.uid != null
            && !this.uid.equals(other.uid))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.model.Users[ uid=" + uid + " ]";
  }

  public User asUser() {
    return new User(uid, username, password, activated, falseLogin, status,isonline);
  }
}
