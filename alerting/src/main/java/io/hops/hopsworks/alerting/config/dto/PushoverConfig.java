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
  "send_resolved",
  "user_key",
  "token",
  "title",
  "message",
  "url",
  "priority",
  "retry",
  "expire",
  "http_config"
  })
public class PushoverConfig {

  @JsonProperty("send_resolved")
  private Boolean sendResolved;
  @JsonProperty("user_key")
  private String userKey;
  @JsonProperty("token")
  private String token;
  @JsonProperty("title")
  private String title;
  @JsonProperty("message")
  private String message;
  @JsonProperty("url")
  private String url;
  @JsonProperty("priority")
  private String priority;
  @JsonProperty("retry")
  private String retry;
  @JsonProperty("expire")
  private String expire;
  @JsonProperty("http_config")
  private HttpConfig httpConfig;

  public PushoverConfig() {
  }

  @JsonProperty("send_resolved")
  public Boolean getSendResolved() {
    return sendResolved;
  }

  @JsonProperty("send_resolved")
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public PushoverConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  @JsonProperty("user_key")
  public String getUserKey() {
    return userKey;
  }

  @JsonProperty("user_key")
  public void setUserKey(String userKey) {
    this.userKey = userKey;
  }

  public PushoverConfig withUserKey(String userKey) {
    this.userKey = userKey;
    return this;
  }

  @JsonProperty("token")
  public String getToken() {
    return token;
  }

  @JsonProperty("token")
  public void setToken(String token) {
    this.token = token;
  }

  public PushoverConfig withToken(String token) {
    this.token = token;
    return this;
  }

  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public PushoverConfig withTitle(String title) {
    this.title = title;
    return this;
  }

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }

  public PushoverConfig withMessage(String message) {
    this.message = message;
    return this;
  }

  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }

  public PushoverConfig withUrl(String url) {
    this.url = url;
    return this;
  }

  @JsonProperty("priority")
  public String getPriority() {
    return priority;
  }

  @JsonProperty("priority")
  public void setPriority(String priority) {
    this.priority = priority;
  }

  public PushoverConfig withPriority(String priority) {
    this.priority = priority;
    return this;
  }

  @JsonProperty("retry")
  public String getRetry() {
    return retry;
  }

  @JsonProperty("retry")
  public void setRetry(String retry) {
    this.retry = retry;
  }

  public PushoverConfig withRetry(String retry) {
    this.retry = retry;
    return this;
  }

  @JsonProperty("expire")
  public String getExpire() {
    return expire;
  }

  @JsonProperty("expire")
  public void setExpire(String expire) {
    this.expire = expire;
  }

  public PushoverConfig withExpire(String expire) {
    this.expire = expire;
    return this;
  }

  @JsonProperty("http_config")
  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  @JsonProperty("http_config")
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
