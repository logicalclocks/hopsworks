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

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement
@ApiModel(value = "Specification to renew an existing JWT")
public class JsonWebTokenDTO {

  private String token;
  @XmlJavaTypeAdapter(DateTimeAdapter.class)
  private Date expiresAt;
  @XmlJavaTypeAdapter(DateTimeAdapter.class)
  private Date nbf;

  public JsonWebTokenDTO() {
  }

  public JsonWebTokenDTO(String token, Date expiresAt, Date nbf) {
    this.token = token;
    this.expiresAt = expiresAt;
    this.nbf = nbf;
  }

  @ApiModelProperty(value = "Token to renew", required = true)
  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  @ApiModelProperty(value = "Expiration date for the new JWT")
  public Date getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
  }

  @ApiModelProperty(value = "Not-valid-before date for the new JWT")
  public Date getNbf() {
    return nbf;
  }

  public void setNbf(Date nbf) {
    this.nbf = nbf;
  }

  @Override
  public String toString() {
    return "JsonWebTokenDTO{" + "token=" + token + ", expiresAt=" + expiresAt + ", nbf=" + nbf + '}';
  }

}
