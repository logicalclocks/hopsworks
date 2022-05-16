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

package io.hops.hopsworks.common.project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AccessCredentialsDTO {
  private String fileExtension;
  private String kStore;
  private String tStore;
  private String password;
  private String caChain;
  private String clientKey;
  private String clientCert;

  public AccessCredentialsDTO() {
  }

  public AccessCredentialsDTO(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  public AccessCredentialsDTO(String fileExtension, String kStore, String tStore, String password, String caChain,
                              String clientCert, String clientKey) {
    this.fileExtension = fileExtension;
    this.kStore = kStore;
    this.tStore = tStore;
    this.password = password;
    this.caChain = caChain;
    this.clientCert = clientCert;
    this.clientKey = clientKey;
  }

  public String getFileExtension() {
    return fileExtension;
  }

  public void setFileExtension(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  public String getkStore() {
    return kStore;
  }

  public void setkStore(String kStore) {
    this.kStore = kStore;
  }

  public String gettStore() {
    return tStore;
  }

  public void settStore(String tStore) {
    this.tStore = tStore;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  
  public String getCaChain() {
    return caChain;
  }
  
  public void setCaChain(String caChain) {
    this.caChain = caChain;
  }
  
  public String getClientKey() {
    return clientKey;
  }
  
  public void setClientKey(String clientKey) {
    this.clientKey = clientKey;
  }
  
  public String getClientCert() {
    return clientCert;
  }
  
  public void setClientCert(String clientCert) {
    this.clientCert = clientCert;
  }
}
