/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.user;

import org.apache.commons.codec.binary.Base64;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class QrCode {
  //added for compatability(old ui)
  private String QRCode;
  private String img;
  private String secret;
  private String issuer;
  private String email;

  public QrCode() {
  }

  public QrCode(byte[] img, String secret, String issuer, String email) {
    this.img = new String(Base64.encodeBase64(img));
    this.QRCode = this.img;
    this.secret = secret;
    this.issuer = issuer;
    this.email = email;
  }

  public String getQRCode() {
    return QRCode;
  }

  public void setQRCode(String QRCode) {
    this.QRCode = QRCode;
  }

  public String getImg() {
    return img;
  }

  public void setImg(String img) {
    this.img = img;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
