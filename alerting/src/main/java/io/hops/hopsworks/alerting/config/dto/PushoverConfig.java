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
public class PushoverConfig {
  @JsonAlias({"send_resolved"})
  private Boolean sendResolved;
  @JsonAlias({"user_key"})
  private String userKey;
  private String token;
  private String title;
  private String message;
  private String url;
  private String priority;
  private String retry;
  private String expire;
  @JsonAlias({"http_config"})
  private HttpConfig httpConfig;

  public PushoverConfig() {
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public PushoverConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  public String getUserKey() {
    return userKey;
  }

  public void setUserKey(String userKey) {
    this.userKey = userKey;
  }

  public PushoverConfig withUserKey(String userKey) {
    this.userKey = userKey;
    return this;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public PushoverConfig withToken(String token) {
    this.token = token;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public PushoverConfig withTitle(String title) {
    this.title = title;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public PushoverConfig withMessage(String message) {
    this.message = message;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public PushoverConfig withUrl(String url) {
    this.url = url;
    return this;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public PushoverConfig withPriority(String priority) {
    this.priority = priority;
    return this;
  }

  public String getRetry() {
    return retry;
  }

  public void setRetry(String retry) {
    this.retry = retry;
  }

  public PushoverConfig withRetry(String retry) {
    this.retry = retry;
    return this;
  }

  public String getExpire() {
    return expire;
  }

  public void setExpire(String expire) {
    this.expire = expire;
  }

  public PushoverConfig withExpire(String expire) {
    this.expire = expire;
    return this;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public PushoverConfig withHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
    return this;
  }

  @Override
  public String toString() {
    return "PushoverConfig{" +
      "sendResolved=" + sendResolved +
      ", userKey='" + userKey + '\'' +
      ", token='" + token + '\'' +
      ", title='" + title + '\'' +
      ", message='" + message + '\'' +
      ", url='" + url + '\'' +
      ", priority='" + priority + '\'' +
      ", retry='" + retry + '\'' +
      ", expire='" + expire + '\'' +
      ", httpConfig=" + httpConfig +
      '}';
  }
}
