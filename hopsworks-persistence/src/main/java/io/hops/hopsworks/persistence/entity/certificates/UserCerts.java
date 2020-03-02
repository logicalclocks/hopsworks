/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.persistence.entity.certificates;

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
