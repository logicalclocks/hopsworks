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
import com.google.common.base.Strings;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "basic_auth",
  "bearer_token",
  "bearer_token_file",
  "tls_config",
  "proxy_url"
  })
/**
 * Note that `basic_auth`, `bearer_token` and `bearer_token_file` options are
 * mutually exclusive.
 */
public class HttpConfig {
  @JsonProperty("basic_auth")
  private BasicAuth basicAuth;
  @JsonProperty("bearer_token")
  private String bearerToken;
  @JsonProperty("bearer_token_file")
  private String bearerTokenFile;
  @JsonProperty("tls_config")
  private TlsConfig tlsConfig;
  @JsonProperty("proxy_url")
  private String proxyUrl;
  
  public HttpConfig() {
  }
  
  @JsonProperty("basic_auth")
  public BasicAuth getBasicAuth() {
    return basicAuth;
  }
  
  @JsonProperty("basic_auth")
  public void setBasicAuth(BasicAuth basicAuth) {
    this.basicAuth = basicAuth;
  }
  
  public HttpConfig withBasicAuth(BasicAuth basicAuth) {
    if (!Strings.isNullOrEmpty(this.bearerToken) || !Strings.isNullOrEmpty(this.bearerTokenFile)) {
      throw new IllegalStateException(
        "`basic_auth`, `bearer_token` and `bearer_token_file` options are mutually exclusive.");
    }
    this.basicAuth = basicAuth;
    return this;
  }
  
  @JsonProperty("bearer_token")
  public String getBearerToken() {
    return bearerToken;
  }
  
  @JsonProperty("bearer_token")
  public void setBearerToken(String bearerToken) {
    this.bearerToken = bearerToken;
  }
  
  public HttpConfig withBearerToken(String bearerToken) {
    if (this.basicAuth != null || !Strings.isNullOrEmpty(this.bearerTokenFile)) {
      throw new IllegalStateException(
        "`basic_auth`, `bearer_token` and `bearer_token_file` options are mutually exclusive.");
    }
    this.bearerToken = bearerToken;
    return this;
  }
  
  @JsonProperty("bearer_token_file")
  public String getBearerTokenFile() {
    return bearerTokenFile;
  }
  
  @JsonProperty("bearer_token_file")
  public void setBearerTokenFile(String bearerTokenFile) {
    this.bearerTokenFile = bearerTokenFile;
  }
  
  public HttpConfig withBearerTokenFile(String bearerTokenFile) {
    if (this.basicAuth != null || !Strings.isNullOrEmpty(this.bearerToken)) {
      throw new IllegalStateException(
        "`basic_auth`, `bearer_token` and `bearer_token_file` options are mutually exclusive.");
    }
    this.bearerTokenFile = bearerTokenFile;
    return this;
  }
  
  @JsonProperty("tls_config")
  public TlsConfig getTlsConfig() {
    return tlsConfig;
  }
  
  @JsonProperty("tls_config")
  public void setTlsConfig(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
  }
  
  public HttpConfig withTlsConfig(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
    return this;
  }
  
  @JsonProperty("proxy_url")
  public String getProxyUrl() {
    return proxyUrl;
  }
  
  @JsonProperty("proxy_url")
  public void setProxyUrl(String proxyUrl) {
    this.proxyUrl = proxyUrl;
  }
  
  public HttpConfig withProxyUrl(String proxyUrl) {
    this.proxyUrl = proxyUrl;
    return this;
  }
  
  @Override
  public String toString() {
    return "HttpConfig{" +
      "basicAuth=" + basicAuth +
      ", bearerToken='" + bearerToken + '\'' +
      ", bearerTokenFile='" + bearerTokenFile + '\'' +
      ", tlsConfig='" + tlsConfig + '\'' +
      ", proxyUrl='" + proxyUrl + '\'' +
      '}';
  }
}
