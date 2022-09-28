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

import io.hops.hopsworks.alerting.config.dto.WechatConfig;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PostableWechatConfig {
  private Boolean sendResolved;
  private String apiSecret;
  private String apiUrl;
  private String corpId;
  private String message;
  private String agentId;
  private String toUser;
  private String toParty;
  private String toTag;

  public PostableWechatConfig() {
  }
  
  public Boolean getSendResolved() {
    return sendResolved;
  }
  
  public void setSendResolved(Boolean sendResolved) {
    this.sendResolved = sendResolved;
  }
  
  public String getApiSecret() {
    return apiSecret;
  }
  
  public void setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
  }
  
  public String getApiUrl() {
    return apiUrl;
  }
  
  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }
  
  public String getCorpId() {
    return corpId;
  }
  
  public void setCorpId(String corpId) {
    this.corpId = corpId;
  }
  
  public String getMessage() {
    return message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  public String getAgentId() {
    return agentId;
  }
  
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }
  
  public String getToUser() {
    return toUser;
  }
  
  public void setToUser(String toUser) {
    this.toUser = toUser;
  }
  
  public String getToParty() {
    return toParty;
  }
  
  public void setToParty(String toParty) {
    this.toParty = toParty;
  }
  
  public String getToTag() {
    return toTag;
  }
  
  public void setToTag(String toTag) {
    this.toTag = toTag;
  }
  
  public WechatConfig toWechatConfig() {
    return new WechatConfig().withSendResolved(this.sendResolved)
      .withApiSecret(this.apiSecret)
      .withApiUrl(this.apiUrl)
      .withCorpId(this.corpId)
      .withMessage(this.message)
      .withAgentId(this.agentId)
      .withToUser(this.toUser)
      .withToParty(this.toParty)
      .withToTag(this.toTag);
  }
}
