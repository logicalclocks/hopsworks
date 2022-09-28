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

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailConfig {

  @JsonAlias({"send_resolved"})
  private Boolean sendResolved;
  private String to;
  private String from;
  private String smarthost;
  private String hello;
  @JsonAlias({"auth_username"})
  private String authUsername;
  @JsonAlias({"auth_password"})
  private String authPassword;
  @JsonAlias({"auth_identity"})
  private String authIdentity;
  @JsonAlias({"auth_secret"})
  private String authSecret;
  @JsonAlias({"require_tls"})
  private Boolean requireTls;
  @JsonAlias({"tls_config"})
  private TlsConfig tlsConfig;
  private String html;
  private String text;
  private Map<String, String> headers;


  public EmailConfig() {
  }

  public EmailConfig(String to) {
    this.to = to;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public EmailConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public EmailConfig withFrom(String from) {
    this.from = from;
    return this;
  }

  public String getSmarthost() {
    return smarthost;
  }

  public void setSmarthost(String smarthost) {
    this.smarthost = smarthost;
  }

  public EmailConfig withSmarthost(String smarthost) {
    this.smarthost = smarthost;
    return this;
  }

  public String getHello() {
    return hello;
  }

  public void setHello(String hello) {
    this.hello = hello;
  }

  public EmailConfig withHello(String hello) {
    this.hello = hello;
    return this;
  }

  public String getAuthUsername() {
    return authUsername;
  }

  public void setAuthUsername(String authUsername) {
    this.authUsername = authUsername;
  }

  public EmailConfig withAuthUsername(String authUsername) {
    this.authUsername = authUsername;
    return this;
  }

  public String getAuthPassword() {
    return authPassword;
  }

  public void setAuthPassword(String authPassword) {
    this.authPassword = authPassword;
  }

  public EmailConfig withAuthPassword(String authPassword) {
    this.authPassword = authPassword;
    return this;
  }

  public String getAuthIdentity() {
    return authIdentity;
  }

  public void setAuthIdentity(String authIdentity) {
    this.authIdentity = authIdentity;
  }

  public EmailConfig withAuthIdentity(String authIdentity) {
    this.authIdentity = authIdentity;
    return this;
  }

  public String getAuthSecret() {
    return authSecret;
  }

  public void setAuthSecret(String authSecret) {
    this.authSecret = authSecret;
  }

  public EmailConfig withAuthSecret(String authSecret) {
    this.authSecret = authSecret;
    return this;
  }

  public Boolean getRequireTls() {
    return requireTls;
  }

  public void setRequireTls(Boolean requireTls) {
    this.requireTls = requireTls;
  }

  public EmailConfig withRequireTls(Boolean requireTls) {
    this.requireTls = requireTls;
    return this;
  }

  public TlsConfig getTlsConfig() {
    return tlsConfig;
  }

  public void setTlsConfig(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
  }

  public EmailConfig withTlsConfig(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
    return this;
  }

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public EmailConfig withHtml(String html) {
    this.html = html;
    return this;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public EmailConfig withText(String text) {
    this.text = text;
    return this;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public EmailConfig withHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailConfig that = (EmailConfig) o;
    return Objects.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(to);
  }

  @Override
  public String toString() {
    return "EmailConfig{" +
      "sendResolved=" + sendResolved +
      ", to='" + to + '\'' +
      ", from='" + from + '\'' +
      ", smarthost='" + smarthost + '\'' +
      ", hello='" + hello + '\'' +
      ", authUsername='" + authUsername + '\'' +
      ", authPassword='" + authPassword + '\'' +
      ", authIdentity='" + authIdentity + '\'' +
      ", authSecret='" + authSecret + '\'' +
      ", requireTls='" + requireTls + '\'' +
      ", tlsConfig=" + tlsConfig +
      ", html='" + html + '\'' +
      ", text='" + text + '\'' +
      ", headers=" + headers +
      '}';
  }
}
