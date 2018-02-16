/*
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
 *
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
