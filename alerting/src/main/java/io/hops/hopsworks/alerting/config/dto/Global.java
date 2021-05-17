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
  "smtp_smarthost",
  "smtp_from",
  "smtp_auth_username",
  "smtp_auth_password",
  "smtp_auth_identity",
  "smtp_auth_secret",
  "smtp_require_tls",
  "slack_api_url",
  "victorops_api_key",
  "victorops_api_url",
  "pagerduty_url",
  "opsgenie_api_key",
  "opsgenie_api_url",
  "wechat_api_url",
  "wechat_api_secret",
  "wechat_api_corp_id",
  "http_config",
  "resolve_timeout"
  })
public class Global {
  
  @JsonProperty("smtp_smarthost")
  private String smtpSmarthost;
  @JsonProperty("smtp_from")
  private String smtpFrom;
  @JsonProperty("smtp_auth_username")
  private String smtpAuthUsername;
  @JsonProperty("smtp_auth_password")
  private String smtpAuthPassword;
  @JsonProperty("smtp_auth_identity")
  private String smtpAuthIdentity;
  @JsonProperty("smtp_auth_secret")
  private String smtpAuthSecret;
  @JsonProperty("smtp_require_tls")
  private String smtpRequireTls;
  
  // The API URL to use for Slack notifications.
  @JsonProperty("slack_api_url")
  private String slackApiUrl;
  
  @JsonProperty("victorops_api_key")
  private String victoropsApiKey;
  @JsonProperty("victorops_api_url")
  private String victoropsApiUrl;
  
  @JsonProperty("pagerduty_url")
  private String pagerdutyUrl;
  
  @JsonProperty("opsgenie_api_key")
  private String opsgenieApiKey;
  @JsonProperty("opsgenie_api_url")
  private String opsgenieApiUrl;
  
  @JsonProperty("wechat_api_url")
  private String wechatApiUrl;
  @JsonProperty("wechat_api_secret")
  private String wechatApiSecret;
  @JsonProperty("wechat_api_corp_id")
  private String wechatApiCorpId;
  //The default HTTP client configuration
  @JsonProperty("http_config")
  private HttpConfig httpConfig;
  //ResolveTimeout is the default value used by alertmanager if the alert does not include EndsAt
  @JsonProperty("resolve_timeout")
  private String resolveTimeout;
  
  /**
   * No args constructor for use in serialization
   */
  public Global() {
  }
  
  /**
   * @param smtpSmarthost
   * @param smtpAuthPassword
   * @param smtpAuthUsername
   * @param smtpFrom
   */
  public Global(String smtpSmarthost, String smtpFrom, String smtpAuthUsername, String smtpAuthPassword) {
    super();
    this.smtpSmarthost = smtpSmarthost;
    this.smtpFrom = smtpFrom;
    this.smtpAuthUsername = smtpAuthUsername;
    this.smtpAuthPassword = smtpAuthPassword;
  }
  
  @JsonProperty("smtp_smarthost")
  public String getSmtpSmarthost() {
    return smtpSmarthost;
  }
  
  @JsonProperty("smtp_smarthost")
  public void setSmtpSmarthost(String smtpSmarthost) {
    this.smtpSmarthost = smtpSmarthost;
  }
  
  public Global withSmtpSmarthost(String smtpSmarthost) {
    this.smtpSmarthost = smtpSmarthost;
    return this;
  }
  
  @JsonProperty("smtp_from")
  public String getSmtpFrom() {
    return smtpFrom;
  }
  
  @JsonProperty("smtp_from")
  public void setSmtpFrom(String smtpFrom) {
    this.smtpFrom = smtpFrom;
  }
  
  public Global withSmtpFrom(String smtpFrom) {
    this.smtpFrom = smtpFrom;
    return this;
  }
  
  @JsonProperty("smtp_auth_username")
  public String getSmtpAuthUsername() {
    return smtpAuthUsername;
  }
  
  @JsonProperty("smtp_auth_username")
  public void setSmtpAuthUsername(String smtpAuthUsername) {
    this.smtpAuthUsername = smtpAuthUsername;
  }
  
  public Global withSmtpAuthUsername(String smtpAuthUsername) {
    this.smtpAuthUsername = smtpAuthUsername;
    return this;
  }
  
  @JsonProperty("smtp_auth_password")
  public String getSmtpAuthPassword() {
    return smtpAuthPassword;
  }
  
  @JsonProperty("smtp_auth_password")
  public void setSmtpAuthPassword(String smtpAuthPassword) {
    this.smtpAuthPassword = smtpAuthPassword;
  }
  
  public Global withSmtpAuthPassword(String smtpAuthPassword) {
    this.smtpAuthPassword = smtpAuthPassword;
    return this;
  }
  
  @JsonProperty("smtp_auth_identity")
  public String getSmtpAuthIdentity() {
    return smtpAuthIdentity;
  }
  
  @JsonProperty("smtp_auth_identity")
  public void setSmtpAuthIdentity(String smtpAuthIdentity) {
    this.smtpAuthIdentity = smtpAuthIdentity;
  }
  
  public Global withSmtpAuthIdentity(String smtpAuthIdentity) {
    this.smtpAuthIdentity = smtpAuthIdentity;
    return this;
  }
  
  @JsonProperty("smtp_auth_secret")
  public String getSmtpAuthSecret() {
    return smtpAuthSecret;
  }
  
  @JsonProperty("smtp_auth_secret")
  public void setSmtpAuthSecret(String smtpAuthSecret) {
    this.smtpAuthSecret = smtpAuthSecret;
  }
  
  public Global withSmtpAuthSecret(String smtpAuthSecret) {
    this.smtpAuthSecret = smtpAuthSecret;
    return this;
  }
  
  @JsonProperty("smtp_require_tls")
  public String getSmtpRequireTls() {
    return smtpRequireTls;
  }
  
  @JsonProperty("smtp_require_tls")
  public void setSmtpRequireTls(String smtpRequireTls) {
    this.smtpRequireTls = smtpRequireTls;
  }
  
  public Global withSmtpRequireTls(String smtpRequireTls) {
    this.smtpRequireTls = smtpRequireTls;
    return this;
  }
  
  @JsonProperty("slack_api_url")
  public String getSlackApiUrl() {
    return slackApiUrl;
  }
  
  @JsonProperty("slack_api_url")
  public void setSlackApiUrl(String slackApiUrl) {
    this.slackApiUrl = slackApiUrl;
  }
  
  public Global withSlackApiUrl(String slackApiUrl) {
    this.slackApiUrl = slackApiUrl;
    return this;
  }
  
  @JsonProperty("victorops_api_key")
  public String getVictoropsApiKey() {
    return victoropsApiKey;
  }
  
  @JsonProperty("victorops_api_key")
  public void setVictoropsApiKey(String victoropsApiKey) {
    this.victoropsApiKey = victoropsApiKey;
  }
  
  public Global withVictoropsApiKey(String victoropsApiKey) {
    this.victoropsApiKey = victoropsApiKey;
    return this;
  }
  
  @JsonProperty("victorops_api_url")
  public String getVictoropsApiUrl() {
    return victoropsApiUrl;
  }
  
  @JsonProperty("victorops_api_url")
  public void setVictoropsApiUrl(String victoropsApiUrl) {
    this.victoropsApiUrl = victoropsApiUrl;
  }
  
  public Global withVictoropsApiUrl(String victoropsApiUrl) {
    this.victoropsApiUrl = victoropsApiUrl;
    return this;
  }
  
  @JsonProperty("pagerduty_url")
  public String getPagerdutyUrl() {
    return pagerdutyUrl;
  }
  
  @JsonProperty("pagerduty_url")
  public void setPagerdutyUrl(String pagerdutyUrl) {
    this.pagerdutyUrl = pagerdutyUrl;
  }
  
  public Global withPagerdutyUrl(String pagerdutyUrl) {
    this.pagerdutyUrl = pagerdutyUrl;
    return this;
  }
  
  @JsonProperty("opsgenie_api_key")
  public String getOpsgenieApiKey() {
    return opsgenieApiKey;
  }
  
  @JsonProperty("opsgenie_api_key")
  public void setOpsgenieApiKey(String opsgenieApiKey) {
    this.opsgenieApiKey = opsgenieApiKey;
  }
  
  public Global withOpsgenieApiKey(String opsgenieApiKey) {
    this.opsgenieApiKey = opsgenieApiKey;
    return this;
  }
  
  @JsonProperty("opsgenie_api_url")
  public String getOpsgenieApiUrl() {
    return opsgenieApiUrl;
  }
  
  @JsonProperty("opsgenie_api_url")
  public void setOpsgenieApiUrl(String opsgenieApiUrl) {
    this.opsgenieApiUrl = opsgenieApiUrl;
  }
  
  public Global withOpsgenieApiUrl(String opsgenieApiUrl) {
    this.opsgenieApiUrl = opsgenieApiUrl;
    return this;
  }
  
  @JsonProperty("wechat_api_url")
  public String getWechatApiUrl() {
    return wechatApiUrl;
  }
  
  @JsonProperty("wechat_api_url")
  public void setWechatApiUrl(String wechatApiUrl) {
    this.wechatApiUrl = wechatApiUrl;
  }
  
  public Global withWechatApiUrl(String wechatApiUrl) {
    this.wechatApiUrl = wechatApiUrl;
    return this;
  }
  
  @JsonProperty("wechat_api_secret")
  public String getWechatApiSecret() {
    return wechatApiSecret;
  }
  
  @JsonProperty("wechat_api_secret")
  public void setWechatApiSecret(String wechatApiSecret) {
    this.wechatApiSecret = wechatApiSecret;
  }
  
  public Global withWechatApiSecret(String wechatApiSecret) {
    this.wechatApiSecret = wechatApiSecret;
    return this;
  }
  
  @JsonProperty("wechat_api_corp_id")
  public String getWechatApiCorpId() {
    return wechatApiCorpId;
  }
  
  @JsonProperty("wechat_api_corp_id")
  public void setWechatApiCorpId(String wechatApiCorpId) {
    this.wechatApiCorpId = wechatApiCorpId;
  }
  
  public Global withWechatApiCorpId(String wechatApiCorpId) {
    this.wechatApiCorpId = wechatApiCorpId;
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
  
  public Global withHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
    return this;
  }
  
  @JsonProperty("resolve_timeout")
  public String getResolveTimeout() {
    return resolveTimeout;
  }
  
  @JsonProperty("resolve_timeout")
  public void setResolveTimeout(String resolveTimeout) {
    this.resolveTimeout = resolveTimeout;
  }
  
  public Global withResolveTimeout(String resolveTimeout) {
    this.resolveTimeout = resolveTimeout;
    return this;
  }
  
  @Override
  public String toString() {
    return "Global{" +
      "smtpSmarthost='" + smtpSmarthost + '\'' +
      ", smtpFrom='" + smtpFrom + '\'' +
      ", smtpAuthUsername='" + smtpAuthUsername + '\'' +
      ", smtpAuthPassword='" + smtpAuthPassword + '\'' +
      ", smtpAuthIdentity='" + smtpAuthIdentity + '\'' +
      ", smtpAuthSecret='" + smtpAuthSecret + '\'' +
      ", smtpRequireTls='" + smtpRequireTls + '\'' +
      ", slackApiUrl='" + slackApiUrl + '\'' +
      ", victoropsApiKey='" + victoropsApiKey + '\'' +
      ", victoropsApiUrl='" + victoropsApiUrl + '\'' +
      ", pagerdutyUrl='" + pagerdutyUrl + '\'' +
      ", opsgenieApiKey='" + opsgenieApiKey + '\'' +
      ", opsgenieApiUrl='" + opsgenieApiUrl + '\'' +
      ", wechatApiUrl='" + wechatApiUrl + '\'' +
      ", wechatApiSecret='" + wechatApiSecret + '\'' +
      ", wechatApiCorpId='" + wechatApiCorpId + '\'' +
      ", httpConfig='" + httpConfig + '\'' +
      ", resolveTimeout='" + resolveTimeout + '\'' +
      '}';
  }
}

