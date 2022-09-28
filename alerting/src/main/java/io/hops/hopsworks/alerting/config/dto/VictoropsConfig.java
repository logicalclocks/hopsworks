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
public class VictoropsConfig {
  @JsonAlias({"send_resolved"})
  private Boolean sendResolved;
  @JsonAlias({"api_key"})
  private String apiKey;
  @JsonAlias({"api_url"})
  private String apiUrl;
  @JsonAlias({"routing_key"})
  private String routingKey;
  @JsonAlias({"message_type"})
  private String messageType;
  @JsonAlias({"entity_display_name"})
  private String entityDisplayName;
  @JsonAlias({"state_message"})
  private String stateMessage;
  @JsonAlias({"monitoring_tool"})
  private String monitoringTool;
  @JsonAlias({"http_config"})
  private HttpConfig httpConfig;

  public VictoropsConfig() {
  }

  public VictoropsConfig(String routingKey) {
    this.routingKey = routingKey;
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public VictoropsConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public VictoropsConfig withApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public VictoropsConfig withApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
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

  public VictoropsConfig withMessageType(String messageType) {
    this.messageType = messageType;
    return this;
  }

  public String getEntityDisplayName() {
    return entityDisplayName;
  }

  public void setEntityDisplayName(String entityDisplayName) {
    this.entityDisplayName = entityDisplayName;
  }

  public VictoropsConfig withEntityDisplayName(String entityDisplayName) {
    this.entityDisplayName = entityDisplayName;
    return this;
  }

  public String getStateMessage() {
    return stateMessage;
  }

  public void setStateMessage(String stateMessage) {
    this.stateMessage = stateMessage;
  }

  public VictoropsConfig withStateMessage(String stateMessage) {
    this.stateMessage = stateMessage;
    return this;
  }

  public String getMonitoringTool() {
    return monitoringTool;
  }

  public void setMonitoringTool(String monitoringTool) {
    this.monitoringTool = monitoringTool;
  }

  public VictoropsConfig withMonitoringTool(String monitoringTool) {
    this.monitoringTool = monitoringTool;
    return this;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public VictoropsConfig withHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
    return this;
  }

  @Override
  public String toString() {
    return "VictoropsConfig{" +
      "sendResolved=" + sendResolved +
      ", apiKey='" + apiKey + '\'' +
      ", apiUrl='" + apiUrl + '\'' +
      ", routingKey='" + routingKey + '\'' +
      ", messageType='" + messageType + '\'' +
      ", entityDisplayName='" + entityDisplayName + '\'' +
      ", stateMessage='" + stateMessage + '\'' +
      ", monitoringTool='" + monitoringTool + '\'' +
      ", httpConfig=" + httpConfig +
      '}';
  }
}
