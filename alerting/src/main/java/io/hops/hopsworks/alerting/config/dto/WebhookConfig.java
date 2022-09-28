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
public class WebhookConfig {
  @JsonAlias({"send_resolved"})
  private Boolean sendResolved;
  private String url;
  @JsonAlias({"max_alerts"})
  private String maxAlerts;
  @JsonAlias({"http_config"})
  private HttpConfig httpConfig;

  public WebhookConfig() {
  }

  public WebhookConfig(String url) {
    this.url = url;
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public WebhookConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getMaxAlerts() {
    return maxAlerts;
  }

  public void setMaxAlerts(String maxAlerts) {
    this.maxAlerts = maxAlerts;
  }

  public WebhookConfig withMaxAlerts(String maxAlerts) {
    this.maxAlerts = maxAlerts;
    return this;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

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
