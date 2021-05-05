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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "ca_file",
  "cert_file",
  "key_file",
  "server_name",
  "insecure_skip_verify"
  })
public class TlsConfig {
  @JsonProperty("ca_file")
  private String caFile;
  @JsonProperty("cert_file")
  private String certFile;
  @JsonProperty("key_file")
  private String keyFile;
  @JsonProperty("server_name")
  private String serverName;
  @JsonProperty("insecure_skip_verify")
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

  @JsonProperty("ca_file")
  public String getCaFile() {
    return caFile;
  }

  @JsonProperty("ca_file")
  public void setCaFile(String caFile) {
    this.caFile = caFile;
  }

  @JsonProperty("cert_file")
  public String getCertFile() {
    return certFile;
  }

  @JsonProperty("cert_file")
  public void setCertFile(String certFile) {
    this.certFile = certFile;
  }

  @JsonProperty("key_file")
  public String getKeyFile() {
    return keyFile;
  }

  @JsonProperty("key_file")
  public void setKeyFile(String keyFile) {
    this.keyFile = keyFile;
  }

  @JsonProperty("server_name")
  public String getServerName() {
    return serverName;
  }

  @JsonProperty("server_name")
  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  @JsonProperty("insecure_skip_verify")
  public String getInsecureSkipVerify() {
    return insecureSkipVerify;
  }

  @JsonProperty("insecure_skip_verify")
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
