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
package io.hops.hopsworks.alerting.config.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TlsConfig {
  @JsonAlias({"ca_file"})
  private String caFile;
  @JsonAlias({"cert_file"})
  private String certFile;
  @JsonAlias({"key_file"})
  private String keyFile;
  @JsonAlias({"server_name"})
  private String serverName;
  @JsonAlias({"insecure_skip_verify"})
  private String insecureSkipVerify;

  public TlsConfig() {
  }

  public TlsConfig(String caFile, String certFile, String keyFile, String serverName, String insecureSkipVerify) {
    this.caFile = caFile;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.serverName = serverName;
    this.insecureSkipVerify = insecureSkipVerify;
  }

  public String getCaFile() {
    return caFile;
  }

  public void setCaFile(String caFile) {
    this.caFile = caFile;
  }

  public String getCertFile() {
    return certFile;
  }

  public void setCertFile(String certFile) {
    this.certFile = certFile;
  }

  public String getKeyFile() {
    return keyFile;
  }

  public void setKeyFile(String keyFile) {
    this.keyFile = keyFile;
  }

  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public String getInsecureSkipVerify() {
    return insecureSkipVerify;
  }

  public void setInsecureSkipVerify(String insecureSkipVerify) {
    this.insecureSkipVerify = insecureSkipVerify;
  }

  @Override
  public String toString() {
    return "TlsConfig{" +
      "caFile='" + caFile + '\'' +
      ", certFile='" + certFile + '\'' +
      ", keyFile='" + keyFile + '\'' +
      ", serverName='" + serverName + '\'' +
      ", insecureSkipVerify='" + insecureSkipVerify + '\'' +
      '}';
  }
}
