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
  "api_secret",
  "api_url",
  "corp_id",
  "message",
  "agent_id",
  "to_user",
  "to_party",
  "to_tag"
  })
public class WechatConfig {
  @JsonProperty("send_resolved")
  private Boolean sendResolved;
  @JsonProperty("api_secret")
  private String apiSecret;
  @JsonProperty("api_url")
  private String apiUrl;
  @JsonProperty("corp_id")
  private String corpId;
  @JsonProperty("message")
  private String message;
  @JsonProperty("agent_id")
  private String agentId;
  @JsonProperty("to_user")
  private String toUser;
  @JsonProperty("to_party")
  private String toParty;
  @JsonProperty("to_tag")
  private String toTag;
  
  public WechatConfig() {
  }
  
  @JsonProperty("send_resolved")
  public Boolean getSendResolved() {
    return sendResolved;
  }
  
  @JsonProperty("send_resolved")
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }
  
  public WechatConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }
  
  @JsonProperty("api_secret")
  public String getApiSecret() {
    return apiSecret;
  }
  
  @JsonProperty("api_secret")
  public void setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
  }
  
  public WechatConfig withApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
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
  
  public WechatConfig withApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }
  
  @JsonProperty("corp_id")
  public String getCorpId() {
    return corpId;
  }
  
  @JsonProperty("corp_id")
  public void setCorpId(String corpId) {
    this.corpId = corpId;
  }
  
  public WechatConfig withCorpId(String corpId) {
    this.corpId = corpId;
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
  
  public WechatConfig withMessage(String message) {
    this.message = message;
    return this;
  }
  
  @JsonProperty("agent_id")
  public String getAgentId() {
    return agentId;
  }
  
  @JsonProperty("agent_id")
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }
  
  public WechatConfig withAgentId(String agentId) {
    this.agentId = agentId;
    return this;
  }
  
  @JsonProperty("to_user")
  public String getToUser() {
    return toUser;
  }
  
  @JsonProperty("to_user")
  public void setToUser(String toUser) {
    this.toUser = toUser;
  }
  
  public WechatConfig withToUser(String toUser) {
    this.toUser = toUser;
    return this;
  }
  
  @JsonProperty("to_party")
  public String getToParty() {
    return toParty;
  }
  
  @JsonProperty("to_party")
  public void setToParty(String toParty) {
    this.toParty = toParty;
  }
  
  public WechatConfig withToParty(String toParty) {
    this.toParty = toParty;
    return this;
  }
  
  @JsonProperty("to_tag")
  public String getToTag() {
    return toTag;
  }
  
  @JsonProperty("to_tag")
  public void setToTag(String toTag) {
    this.toTag = toTag;
  }
  
  public WechatConfig withToTag(String toTag) {
    this.toTag = toTag;
    return this;
  }
  
  @Override
  public String toString() {
    return "WechatConfig{" +
      "sendResolved=" + sendResolved +
      ", apiSecret='" + apiSecret + '\'' +
      ", apiUrl='" + apiUrl + '\'' +
      ", corpId='" + corpId + '\'' +
      ", message='" + message + '\'' +
      ", agentId='" + agentId + '\'' +
      ", toUser='" + toUser + '\'' +
      ", toParty='" + toParty + '\'' +
      ", toTag='" + toTag + '\'' +
      '}';
  }
}
