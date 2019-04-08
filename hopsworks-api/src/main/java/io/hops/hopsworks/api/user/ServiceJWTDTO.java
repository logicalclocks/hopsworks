/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.user;

import io.hops.hopsworks.api.jwt.JWTResponseDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServiceJWTDTO {
  
  private JWTResponseDTO jwt;
  private String[] renewTokens;
  
  public ServiceJWTDTO() {
  }
  
  public ServiceJWTDTO(JWTResponseDTO jwt, String[] renewTokens) {
    this.jwt = jwt;
    this.renewTokens = renewTokens;
  }
  
  public JWTResponseDTO getJwt() {
    return jwt;
  }
  
  public void setJwt(JWTResponseDTO jwt) {
    this.jwt = jwt;
  }
  
  public String[] getRenewTokens() {
    return renewTokens;
  }
  
  public void setRenewTokens(String[] renewTokens) {
    this.renewTokens = renewTokens;
  }
}
