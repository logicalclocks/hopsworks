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
  "api_key",
  "api_url",
  "routing_key",
  "message_type",
  "entity_display_name",
  "state_message",
  "monitoring_tool",
  "http_config"
  })
public class VictoropsConfig {
  @JsonProperty("send_resolved")
  private Boolean sendResolved;
  @JsonProperty("api_key")
  private String apiKey;
  @JsonProperty("api_url")
  private String apiUrl;
  @JsonProperty("routing_key")
  private String routingKey;
  @JsonProperty("message_type")
  private String messageType;
  @JsonProperty("entity_display_name")
  private String entityDisplayName;
  @JsonProperty("state_message")
  private String stateMessage;
  @JsonProperty("monitoring_tool")
  private String monitoringTool;
  @JsonProperty("http_config")
  private HttpConfig httpConfig;

  public VictoropsConfig() {
  }

  public VictoropsConfig(String routingKey) {
    this.routingKey = routingKey;
  }

  @JsonProperty("send_resolved")
  public Boolean getSendResolved() {
    return sendResolved;
  }

  @JsonProperty("send_resolved")
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public VictoropsConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  @JsonProperty("api_key")
  public String getApiKey() {
    return apiKey;
  }

  @JsonProperty("api_key")
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public VictoropsConfig withApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  @JsonProperty("api_url")
  public String getApiUrl() {
    return apiUrl;
  }

  @JsonProperty("api_url")
  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public VictoropsConfig withApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  @JsonProperty("routing_key")
  public String getRoutingKey() {
    return routingKey;
  }

  @JsonProperty("routing_key")
  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  @JsonProperty("message_type")
  public String getMessageType() {
    return messageType;
  }

  @JsonProperty("message_type")
  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public VictoropsConfig withMessageType(String messageType) {
    this.messageType = messageType;
    return this;
  }

  @JsonProperty("entity_display_name")
  public String getEntityDisplayName() {
    return entityDisplayName;
  }

  @JsonProperty("entity_display_name")
  public void setEntityDisplayName(String entityDisplayName) {
    this.entityDisplayName = entityDisplayName;
  }

  public VictoropsConfig withEntityDisplayName(String entityDisplayName) {
    this.entityDisplayName = entityDisplayName;
    return this;
  }

  @JsonProperty("state_message")
  public String getStateMessage() {
    return stateMessage;
  }

  @JsonProperty("state_message")
  public void setStateMessage(String stateMessage) {
    this.stateMessage = stateMessage;
  }

  public VictoropsConfig withStateMessage(String stateMessage) {
    this.stateMessage = stateMessage;
    return this;
  }

  @JsonProperty("monitoring_tool")
  public String getMonitoringTool() {
    return monitoringTool;
  }

  @JsonProperty("monitoring_tool")
  public void setMonitoringTool(String monitoringTool) {
    this.monitoringTool = monitoringTool;
  }

  public VictoropsConfig withMonitoringTool(String monitoringTool) {
    this.monitoringTool = monitoringTool;
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
