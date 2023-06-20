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
import com.google.common.base.Strings;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpConfig {
  private static final String ERROR_MSG = "`basic_auth`, `bearer_token` and `bearer_token_file` options are mutually" +
    " exclusive.";
  @JsonAlias({"basic_auth"})
  private BasicAuth basicAuth;
  @JsonAlias({"bearer_token"})
  private String bearerToken;
  @JsonAlias({"bearer_token_file"})
  private String bearerTokenFile;
  @JsonAlias({"tls_config"})
  private TlsConfig tlsConfig;
  @JsonAlias({"proxy_url"})
  private String proxyUrl;

  public HttpConfig() {
  }

  public BasicAuth getBasicAuth() {
    return basicAuth;
  }

  public void setBasicAuth(BasicAuth basicAuth) {
    this.basicAuth = basicAuth;
  }

  public HttpConfig withBasicAuth(BasicAuth basicAuth) {
    if (!Strings.isNullOrEmpty(this.bearerToken) || !Strings.isNullOrEmpty(this.bearerTokenFile)) {
      throw new IllegalStateException(ERROR_MSG);
    }
    this.basicAuth = basicAuth;
    return this;
  }

  public String getBearerToken() {
    return bearerToken;
  }

  public void setBearerToken(String bearerToken) {
    this.bearerToken = bearerToken;
  }

  public HttpConfig withBearerToken(String bearerToken) {
    if (this.basicAuth != null || !Strings.isNullOrEmpty(this.bearerTokenFile)) {
      throw new IllegalStateException(ERROR_MSG);
    }
    this.bearerToken = bearerToken;
    return this;
  }

  public String getBearerTokenFile() {
    return bearerTokenFile;
  }

  public void setBearerTokenFile(String bearerTokenFile) {
    this.bearerTokenFile = bearerTokenFile;
  }

  public HttpConfig withBearerTokenFile(String bearerTokenFile) {
    if (this.basicAuth != null || !Strings.isNullOrEmpty(this.bearerToken)) {
      throw new IllegalStateException(ERROR_MSG);
    }
    this.bearerTokenFile = bearerTokenFile;
    return this;
  }

  public TlsConfig getTlsConfig() {
    return tlsConfig;
  }

  public void setTlsConfig(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
  }

  public HttpConfig withTlsConfig(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
    return this;
  }

  public String getProxyUrl() {
    return proxyUrl;
  }

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
