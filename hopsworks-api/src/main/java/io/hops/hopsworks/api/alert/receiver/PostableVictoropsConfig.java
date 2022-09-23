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
import io.hops.hopsworks.alerting.config.dto.VictoropsConfig;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PostableVictoropsConfig {
  private Boolean sendResolved;
  private String apiKey;
  private String apiUrl;
  private String routingKey;
  private String messageType;
  private String entityDisplayName;
  private String stateMessage;
  private String monitoringTool;
  private HttpConfig httpConfig;

  public PostableVictoropsConfig() {
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public String getEntityDisplayName() {
    return entityDisplayName;
  }

  public void setEntityDisplayName(String entityDisplayName) {
    this.entityDisplayName = entityDisplayName;
  }

  public String getStateMessage() {
    return stateMessage;
  }

  public void setStateMessage(String stateMessage) {
    this.stateMessage = stateMessage;
  }

  public String getMonitoringTool() {
    return monitoringTool;
  }

  public void setMonitoringTool(String monitoringTool) {
    this.monitoringTool = monitoringTool;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public VictoropsConfig toVictoropsConfig() {
    return new VictoropsConfig(this.routingKey).withApiKey(this.apiKey)
      .withApiUrl(this.apiUrl)
      .withSendResolved(this.sendResolved)
      .withMessageType(this.messageType)
      .withEntityDisplayName(this.entityDisplayName)
      .withStateMessage(this.stateMessage)
      .withMonitoringTool(this.monitoringTool)
      .withHttpConfig(this.httpConfig);
  }
}
