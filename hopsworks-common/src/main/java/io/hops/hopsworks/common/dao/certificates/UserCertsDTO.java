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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserCertsDTO {

  private byte[] userKey;
  private byte[] userCert;
  private String userKeyPwd;
  private String userCertPwd;

  public UserCertsDTO() {
  }

  public UserCertsDTO(byte[] userKey, byte[] userCert, String userKeyPwd, String userCertPwd) {
    this.userKey = userKey;
    this.userCert = userCert;
    this.userKeyPwd = userKeyPwd;
    this.userCertPwd = userCertPwd;
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

  public String getUserCertPwd() {
    return userCertPwd;
  }

  public void setUserCertPwd(String userCertPwd) {
    this.userCertPwd = userCertPwd;
  }

}
