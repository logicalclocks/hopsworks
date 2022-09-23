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
import io.hops.hopsworks.alerting.config.dto.WebhookConfig;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PostableWebhookConfig {
  private Boolean sendResolved;
  private String url;
  private String maxAlerts;
  private HttpConfig httpConfig;

  public PostableWebhookConfig() {
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
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

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public WebhookConfig toWebhookConfig() {
    return new WebhookConfig(this.url).withSendResolved(this.sendResolved)
      .withMaxAlerts(this.maxAlerts)
      .withHttpConfig(this.httpConfig);
  }
}
