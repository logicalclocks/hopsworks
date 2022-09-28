/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

import io.hops.hopsworks.alerting.config.dto.HttpConfig;
import io.hops.hopsworks.alerting.config.dto.PushoverConfig;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PostablePushoverConfig {
  private Boolean sendResolved;
  private String userKey;
  private String token;
  private String title;
  private String message;
  private String url;
  private String priority;
  private String retry;
  private String expire;
  private HttpConfig httpConfig;

  public PostablePushoverConfig() {
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public String getUserKey() {
    return userKey;
  }

  public void setUserKey(String userKey) {
    this.userKey = userKey;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getRetry() {
    return retry;
  }

  public void setRetry(String retry) {
    this.retry = retry;
  }

  public String getExpire() {
    return expire;
  }

  public void setExpire(String expire) {
    this.expire = expire;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public PushoverConfig toPushoverConfig() {
    return new PushoverConfig().withSendResolved(this.sendResolved)
      .withUserKey(this.userKey)
      .withToken(this.token)
      .withTitle(this.title)
      .withMessage(this.message)
      .withUrl(this.url)
      .withPriority(this.priority)
      .withRetry(this.retry)
      .withExpire(this.expire)
      .withHttpConfig(this.httpConfig);
  }
}
