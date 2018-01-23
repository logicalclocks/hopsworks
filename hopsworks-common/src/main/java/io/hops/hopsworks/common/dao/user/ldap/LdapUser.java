package io.hops.hopsworks.common.dao.user.ldap;

import io.hops.hopsworks.common.dao.user.Users;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "hopsworks.ldap_user")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "LdapUser.findAll",
      query = "SELECT l FROM LdapUser l")
  ,
  @NamedQuery(name = "LdapUser.findByUid",
      query = "SELECT l FROM LdapUser l WHERE l.uid = :uid")
  ,
  @NamedQuery(name = "LdapUser.findByEntryUuid",
      query = "SELECT l FROM LdapUser l WHERE l.entryUuid = :entryUuid")})
public class LdapUser implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 128)
  @Column(name = "entry_uuid")
  private String entryUuid;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 64)
  @Column(name = "auth_key")
  private String authKey;
  @JoinColumn(name = "uid",
      referencedColumnName = "uid")
  @OneToOne(optional = false, cascade = CascadeType.PERSIST)
  private Users uid;

  public LdapUser() {
  }

  public LdapUser(String entryUuid) {
    this.entryUuid = entryUuid;
  }

  public LdapUser(String entryUuid, Users uid, String authKey) {
    this.entryUuid = entryUuid;
    this.uid = uid;
    this.authKey = authKey;
  }

  public String getEntryUuid() {
    return entryUuid;
  }

  public void setEntryUuid(String entryUuid) {
    this.entryUuid = entryUuid;
  }

  @XmlTransient
  @JsonIgnore
  public String getAuthKey() {
    return authKey;
  }

  public void setAuthKey(String authKey) {
    this.authKey = authKey;
  }

  public Users getUid() {
    return uid;
  }

  public void setUid(Users uid) {
    this.uid = uid;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (entryUuid != null ? entryUuid.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof LdapUser)) {
      return false;
    }
    LdapUser other = (LdapUser) object;
    if ((this.entryUuid == null && other.entryUuid != null) ||
        (this.entryUuid != null && !this.entryUuid.equals(other.entryUuid))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.user.ldap.LdapUser[ entryUuid=" + entryUuid + " ]";
  }
  
}
