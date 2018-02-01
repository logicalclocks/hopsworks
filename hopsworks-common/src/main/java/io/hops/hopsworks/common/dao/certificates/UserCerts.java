/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.certificates;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "user_certs",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "UserCerts.findAll",
          query = "SELECT u FROM UserCerts u"),
  @NamedQuery(name = "UserCerts.findByProjectname",
          query
          = "SELECT u FROM UserCerts u WHERE u.userCertsPK.projectname = :projectname"),
  @NamedQuery(name = "UserCerts.findByUsername",
          query
          = "SELECT u FROM UserCerts u WHERE u.userCertsPK.username = :username"),
  @NamedQuery(name = "UserCerts.findUserProjectCert",
          query
          = "SELECT u FROM UserCerts u WHERE u.userCertsPK.username = :username "
          + "AND u.userCertsPK.projectname = :projectname")})
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
  @Size(min = 1, 
        max = 200)
  @Column(name = "user_key_pwd")
  private String userKeyPwd;
  
  public UserCerts() {
  }

  public UserCerts(UserCertsPK userCertsPK) {
    this.userCertsPK = userCertsPK;
  }

  public UserCerts(String projectname, String username) {
    this.userCertsPK = new UserCertsPK(projectname, username);
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

  public String getUserKeyPwd() {
    return userKeyPwd;
  }

  public void setUserKeyPwd(String userKeyPwd) {
    this.userKeyPwd = userKeyPwd;
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
    return !((this.userCertsPK == null && other.userCertsPK != null) || 
        (this.userCertsPK != null && !this.userCertsPK.equals(other.userCertsPK)));
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.certificates.UserCerts[ userCertsPK=" + userCertsPK
            + " ]";
  }

}
