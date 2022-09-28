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
package io.hops.hopsworks.api.alert.receiver;

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.TlsConfig;
import io.hops.hopsworks.api.alert.Entry;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@XmlRootElement
public class PostableEmailConfig {
  private Boolean sendResolved;
  private String to;
  private String from;
  private String smarthost;
  private String hello;
  private String authUsername;
  private String authPassword;
  private String authIdentity;
  private String authSecret;
  private Boolean requireTls;
  private TlsConfig tlsConfig;
  private String html;
  private String text;
  private List<Entry> headers;

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getSmarthost() {
    return smarthost;
  }

  public void setSmarthost(String smarthost) {
    this.smarthost = smarthost;
  }

  public String getHello() {
    return hello;
  }

  public void setHello(String hello) {
    this.hello = hello;
  }

  public String getAuthUsername() {
    return authUsername;
  }

  public void setAuthUsername(String authUsername) {
    this.authUsername = authUsername;
  }

  public String getAuthPassword() {
    return authPassword;
  }

  public void setAuthPassword(String authPassword) {
    this.authPassword = authPassword;
  }

  public String getAuthIdentity() {
    return authIdentity;
  }

  public void setAuthIdentity(String authIdentity) {
    this.authIdentity = authIdentity;
  }

  public String getAuthSecret() {
    return authSecret;
  }

  public void setAuthSecret(String authSecret) {
    this.authSecret = authSecret;
  }

  public Boolean getRequireTls() {
    return requireTls;
  }

  public void setRequireTls(Boolean requireTls) {
    this.requireTls = requireTls;
  }

  public TlsConfig getTlsConfig() {
    return tlsConfig;
  }

  public void setTlsConfig(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
  }

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public List<Entry> getHeaders() {
    return headers;
  }

  public void setHeaders(List<Entry> headers) {
    this.headers = headers;
  }

  public EmailConfig toEmailConfig(Boolean defaultTemplate) {
    EmailConfig emailConfig = new EmailConfig(this.getTo())
      .withFrom(this.getFrom())
      .withSmarthost(this.getSmarthost())
      .withHello(this.getHello())
      .withAuthIdentity(this.getAuthIdentity())
      .withAuthPassword(this.getAuthPassword())
      .withAuthSecret(this.getAuthSecret())
      .withAuthUsername(this.getAuthUsername())
      .withHtml(this.getHtml())
      .withText(this.getText())
      .withRequireTls(this.getRequireTls())
      .withTlsConfig(this.getTlsConfig())
      .withSendResolved(this.getSendResolved());
    if (this.getHeaders() != null && !this.getHeaders().isEmpty()) {
      Map<String, String> headers;
      headers = this.getHeaders().stream()
        .filter(entry -> entry != null && entry.getKey() != null && entry.getValue() != null)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      emailConfig.setHeaders(headers);
    }
    if (defaultTemplate && Strings.isNullOrEmpty(emailConfig.getHtml())) {
      emailConfig.setHtml(Constants.DEFAULT_EMAIL_HTML);
    }
    return emailConfig;
  }
}
