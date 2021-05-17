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

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "send_resolved",
  "to",
  "from",
  "smarthost",
  "hello",
  "auth_username",
  "auth_password",
  "auth_identity",
  "auth_secret",
  "require_tls",
  "tls_config",
  "html",
  "text",
  "headers"
  })
public class EmailConfig {
  
  @JsonProperty("send_resolved")
  private Boolean sendResolved;
  @JsonProperty("to")
  private String to;
  @JsonProperty("from")
  private String from;
  @JsonProperty("smarthost")
  private String smarthost;
  @JsonProperty("hello")
  private String hello;
  @JsonProperty("auth_username")
  private String authUsername;
  @JsonProperty("auth_password")
  private String authPassword;
  @JsonProperty("auth_identity")
  private String authIdentity;
  @JsonProperty("auth_secret")
  private String authSecret;
  @JsonProperty("require_tls")
  private Boolean requireTls;
  @JsonProperty("tls_config")
  private TlsConfig tlsConfig;
  @JsonProperty("html")
  private String html;
  @JsonProperty("text")
  private String text;
  @JsonProperty("headers")
  private Map<String, String> headers;
  
  
  public EmailConfig() {
  }

  public EmailConfig(String to) {
    this.to = to;
  }
  
  @JsonProperty("to")
  public String getTo() {
    return to;
  }
  
  @JsonProperty("to")
  public void setTo(String to) {
    this.to = to;
  }
  
  @JsonProperty("send_resolved")
  public Boolean getSendResolved() {
    return sendResolved;
  }
  
  @JsonProperty("send_resolved")
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }
  
  public EmailConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }
  
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }
  
  @JsonProperty("from")
  public void setFrom(String from) {
    this.from = from;
  }
  
  public EmailConfig withFrom(String from) {
    this.from = from;
    return this;
  }
  
  @JsonProperty("smarthost")
  public String getSmarthost() {
    return smarthost;
  }
  
  @JsonProperty("smarthost")
  public void setSmarthost(String smarthost) {
    this.smarthost = smarthost;
  }
  
  public EmailConfig withSmarthost(String smarthost) {
    this.smarthost = smarthost;
    return this;
  }
  
  @JsonProperty("hello")
  public String getHello() {
    return hello;
  }
  
  @JsonProperty("hello")
  public void setHello(String hello) {
    this.hello = hello;
  }
  
  public EmailConfig withHello(String hello) {
    this.hello = hello;
    return this;
  }
  
  @JsonProperty("auth_username")
  public String getAuthUsername() {
    return authUsername;
  }
  
  @JsonProperty("auth_username")
  public void setAuthUsername(String authUsername) {
    this.authUsername = authUsername;
  }
  
  public EmailConfig withAuthUsername(String authUsername) {
    this.authUsername = authUsername;
    return this;
  }
  
  @JsonProperty("auth_password")
  public String getAuthPassword() {
    return authPassword;
  }
  
  @JsonProperty("auth_password")
  public void setAuthPassword(String authPassword) {
    this.authPassword = authPassword;
  }
  
  public EmailConfig withAuthPassword(String authPassword) {
    this.authPassword = authPassword;
    return this;
  }
  
  @JsonProperty("auth_identity")
  public String getAuthIdentity() {
    return authIdentity;
  }
  
  @JsonProperty("auth_identity")
  public void setAuthIdentity(String authIdentity) {
    this.authIdentity = authIdentity;
  }
  
  public EmailConfig withAuthIdentity(String authIdentity) {
    this.authIdentity = authIdentity;
    return this;
  }
  
  @JsonProperty("auth_secret")
  public String getAuthSecret() {
    return authSecret;
  }
  
  @JsonProperty("auth_secret")
  public void setAuthSecret(String authSecret) {
    this.authSecret = authSecret;
  }
  
  public EmailConfig withAuthSecret(String authSecret) {
    this.authSecret = authSecret;
    return this;
  }
  
  @JsonProperty("require_tls")
  public Boolean getRequireTls() {
    return requireTls;
  }
  
  @JsonProperty("require_tls")
  public void setRequireTls(Boolean requireTls) {
    this.requireTls = requireTls;
  }
  
  public EmailConfig withRequireTls(Boolean requireTls) {
    this.requireTls = requireTls;
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
  
  public EmailConfig withTlsConfig(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
    return this;
  }
  
  @JsonProperty("html")
  public String getHtml() {
    return html;
  }
  
  @JsonProperty("html")
  public void setHtml(String html) {
    this.html = html;
  }
  
  public EmailConfig withHtml(String html) {
    this.html = html;
    return this;
  }
  
  @JsonProperty("text")
  public String getText() {
    return text;
  }
  
  @JsonProperty("text")
  public void setText(String text) {
    this.text = text;
  }
  
  public EmailConfig withText(String text) {
    this.text = text;
    return this;
  }
  
  @JsonProperty("headers")
  public Map<String, String> getHeaders() {
    return headers;
  }
  
  @JsonProperty("headers")
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
