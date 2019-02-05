/*
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
 */
package io.hops.hopsworks.api.jwt;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JWTRequestDTO {

  private String subject;
  private String[] audiences;
  private String keyName;
  private Date expiresAt;
  private Date notBefore;
  private boolean renewable;
  private int expLeeway;

  public JWTRequestDTO() {
  }

  public JWTRequestDTO(String subject, String[] audiences, String keyName) {
    this.subject = subject;
    this.audiences = audiences;
    this.keyName = keyName;
  }

  public JWTRequestDTO(String subject, String[] audiences, String keyName, Date expiresAt, Date notBefore,
      boolean renewable, int expLeeway) {
    this.subject = subject;
    this.audiences = audiences;
    this.keyName = keyName;
    this.expiresAt = expiresAt;
    this.notBefore = notBefore;
    this.renewable = renewable;
    this.expLeeway = expLeeway;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String[] getAudiences() {
    return audiences;
  }

  public void setAudiences(String[] audiences) {
    this.audiences = audiences;
  }

  public String getKeyName() {
    return keyName;
  }

  public void setKeyName(String keyName) {
    this.keyName = keyName;
  }

  public Date getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Date getNotBefore() {
    return notBefore;
  }

  public void setNotBefore(Date notBefore) {
    this.notBefore = notBefore;
  }

  public boolean isRenewable() {
    return renewable;
  }

  public void setRenewable(boolean renewable) {
    this.renewable = renewable;
  }

  public int getExpLeeway() {
    return expLeeway;
  }

  public void setExpLeeway(int expLeeway) {
    this.expLeeway = expLeeway;
  }

  @Override
  public String toString() {
    return "JWTRequestDTO{" + "subject=" + subject + ", audiences=" + audiences + ", keyName=" + keyName
        + ", expiresAt=" + expiresAt + ", notBefore=" + notBefore + ", renewable=" + renewable + ", expLeeway="
        + expLeeway + '}';
  }

}
