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
  "url",
  "max_alerts",
  "http_config"
  })
public class WebhookConfig {
  @JsonProperty("send_resolved")
  private Boolean sendResolved;
  @JsonProperty("url")
  private String url;
  @JsonProperty("max_alerts")
  private String maxAlerts;
  @JsonProperty("http_config")
  private HttpConfig httpConfig;

  public WebhookConfig() {
  }

  public WebhookConfig(String url) {
    this.url = url;
  }

  @JsonProperty("send_resolved")
  public Boolean getSendResolved() {
    return sendResolved;
  }

  @JsonProperty("send_resolved")
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public WebhookConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
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

  @JsonProperty("max_alerts")
  public String getMaxAlerts() {
    return maxAlerts;
  }

  @JsonProperty("max_alerts")
  public void setMaxAlerts(String maxAlerts) {
    this.maxAlerts = maxAlerts;
  }

  public WebhookConfig withMaxAlerts(String maxAlerts) {
    this.maxAlerts = maxAlerts;
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

  public WebhookConfig withHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
    return this;
  }

  @Override
  public String toString() {
    return "WebhookConfig{" +
      "sendResolved=" + sendResolved +
      ", url='" + url + '\'' +
      ", maxAlerts='" + maxAlerts + '\'' +
      ", httpConfig=" + httpConfig +
      '}';
  }
}
