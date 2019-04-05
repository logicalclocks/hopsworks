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
package io.hops.hopsworks.api.util;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OpenIdProvider {
  private String providerName;
  private String providerDisplayName;
  private String providerLogoURI;
  
  public OpenIdProvider() {
  }
  
  public OpenIdProvider(String providerName, String providerDisplayName, String providerLogoURI) {
    this.providerName = providerName;
    this.providerDisplayName = providerDisplayName;
    this.providerLogoURI = providerLogoURI;
  }
  
  public String getProviderName() {
    return providerName;
  }
  
  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }
  
  public String getProviderDisplayName() {
    return providerDisplayName;
  }
  
  public void setProviderDisplayName(String providerDisplayName) {
    this.providerDisplayName = providerDisplayName;
  }
  
  public String getProviderLogoURI() {
    return providerLogoURI;
  }
  
  public void setProviderLogoURI(String providerLogoURI) {
    this.providerLogoURI = providerLogoURI;
  }
}
