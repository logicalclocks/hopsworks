/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.certificates;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "user_certs", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "UserCerts.findAll", query = "SELECT u FROM UserCerts u"),
  @NamedQuery(name = "UserCerts.findByProjectname", query = "SELECT u FROM UserCerts u WHERE u.userCertsPK.projectname = :projectname"),
  @NamedQuery(name = "UserCerts.findByUserId", query = "SELECT u FROM UserCerts u WHERE u.userCertsPK.userId = :userId"),
  @NamedQuery(name = "UserCerts.findUserProjectCert", query = "SELECT u FROM UserCerts u WHERE u.userCertsPK.userId = :userId AND u.userCertsPK.projectname = :projectname")
})
public class UserCerts implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected UserCertsPK userCertsPK;
  @Lob
  @Column(name = "user_key")
  private byte[] userKey;
  @Lob
  @Column(name = "user_cert")
  private byte[] userCert;

  public UserCerts() {
  }

  public UserCerts(UserCertsPK userCertsPK) {
    this.userCertsPK = userCertsPK;
  }

  public UserCerts(String projectname, int userId) {
    this.userCertsPK = new UserCertsPK(projectname, userId);
  }

  public UserCertsPK getUserCertsPK() {
    return userCertsPK;
  }

  public void setUserCertsPK(UserCertsPK userCertsPK) {
    this.userCertsPK = userCertsPK;
  }

  public byte[] getUserKey() {
    return userKey;
  }

  public void setUserKey(byte[] userKey) {
    this.userKey = userKey;
  }

  public byte[] getUserCert() {
    return userCert;
  }

  public void setUserCert(byte[] userCert) {
    this.userCert = userCert;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (userCertsPK != null ? userCertsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof UserCerts)) {
      return false;
    }
    UserCerts other = (UserCerts) object;
    if ((this.userCertsPK == null && other.userCertsPK != null) || (this.userCertsPK != null && !this.userCertsPK.equals(other.userCertsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.certificates.UserCerts[ userCertsPK=" + userCertsPK + " ]";
  }
  
}
