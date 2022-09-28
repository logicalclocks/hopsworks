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
public class WechatConfig {
  @JsonAlias({"send_resolved"})
  private Boolean sendResolved;
  @JsonAlias({"api_secret"})
  private String apiSecret;
  @JsonAlias({"api_url"})
  private String apiUrl;
  @JsonAlias({"corp_id"})
  private String corpId;
  private String message;
  @JsonAlias({"agent_id"})
  private String agentId;
  @JsonAlias({"to_user"})
  private String toUser;
  @JsonAlias({"to_party"})
  private String toParty;
  @JsonAlias({"to_tag"})
  private String toTag;

  public WechatConfig() {
  }

  public Boolean getSendResolved() {
    return sendResolved;
  }

  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }

  public WechatConfig withSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
    return this;
  }

  public String getApiSecret() {
    return apiSecret;
  }

  public void setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
  }

  public WechatConfig withApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
    return this;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public WechatConfig withApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  public String getCorpId() {
    return corpId;
  }

  public void setCorpId(String corpId) {
    this.corpId = corpId;
  }

  public WechatConfig withCorpId(String corpId) {
    this.corpId = corpId;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public WechatConfig withMessage(String message) {
    this.message = message;
    return this;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public WechatConfig withAgentId(String agentId) {
    this.agentId = agentId;
    return this;
  }

  public String getToUser() {
    return toUser;
  }

  public void setToUser(String toUser) {
    this.toUser = toUser;
  }

  public WechatConfig withToUser(String toUser) {
    this.toUser = toUser;
    return this;
  }

  public String getToParty() {
    return toParty;
  }

  public void setToParty(String toParty) {
    this.toParty = toParty;
  }

  public WechatConfig withToParty(String toParty) {
    this.toParty = toParty;
    return this;
  }

  public String getToTag() {
    return toTag;
  }

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
