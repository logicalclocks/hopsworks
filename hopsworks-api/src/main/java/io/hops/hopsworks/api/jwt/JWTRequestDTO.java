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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel(value = "Specification for generating new JWT")
public class JWTRequestDTO {

  private String subject;
  @XmlJavaTypeAdapter(ArrayAdapter.class)
  private String[] audiences;
  private String keyName;
  @XmlJavaTypeAdapter(DateTimeAdapter.class)
  private Date expiresAt;
  @XmlJavaTypeAdapter(DateTimeAdapter.class)
  private Date nbf;
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
    this.nbf = notBefore;
    this.renewable = renewable;
    this.expLeeway = expLeeway;
  }

  @ApiModelProperty(value = "Subject to be encoded in JWT", required = true)
  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  @ApiModelProperty(value = "Appropriate audience for the JWT", required = true)
  public String[] getAudiences() {
    return audiences;
  }

  public void setAudiences(String[] audiences) {
    this.audiences = audiences;
  }

  @ApiModelProperty(value = "Name of the signing key", required = true)
  public String getKeyName() {
    return keyName;
  }

  public void setKeyName(String keyName) {
    this.keyName = keyName;
  }

  @ApiModelProperty(value = "Expiration date of the token")
  public Date getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
  }

  @ApiModelProperty(value = "Not-valid-before date")
  public Date getNbf() {
    return nbf;
  }

  public void setNbf(Date notBefore) {
    this.nbf = notBefore;
  }

  @ApiModelProperty(value = "Flag to indicate if the token is auto-renewable", required = true)
  public boolean isRenewable() {
    return renewable;
  }

  public void setRenewable(boolean renewable) {
    this.renewable = renewable;
  }

  @ApiModelProperty(value = "Number of seconds after the expiration the token is still valid", required = true)
  public int getExpLeeway() {
    return expLeeway;
  }

  public void setExpLeeway(int expLeeway) {
    this.expLeeway = expLeeway;
  }

  @Override
  public String toString() {
    return "JWTRequestDTO{" + "subject=" + subject + ", audiences=" + audiences + ", keyName=" + keyName
        + ", expiresAt=" + expiresAt + ", notBefore=" + nbf + ", renewable=" + renewable + ", expLeeway="
        + expLeeway + '}';
  }

}
