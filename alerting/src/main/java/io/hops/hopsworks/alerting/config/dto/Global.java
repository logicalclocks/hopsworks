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
public class Global {

  @JsonAlias({"smtp_smarthost"})
  private String smtpSmarthost;
  @JsonAlias({"smtp_from"})
  private String smtpFrom;
  @JsonAlias({"smtp_auth_username"})
  private String smtpAuthUsername;
  @JsonAlias({"smtp_auth_password"})
  private String smtpAuthPassword;
  @JsonAlias({"smtp_auth_identity"})
  private String smtpAuthIdentity;
  @JsonAlias({"smtp_auth_secret"})
  private String smtpAuthSecret;
  @JsonAlias({"smtp_require_tls"})
  private String smtpRequireTls;
  @JsonAlias({"smtp_hello"})
  private String smtpHello;

  // The API URL to use for Slack notifications.
  @JsonAlias({"slack_api_url"})
  private String slackApiUrl;

  @JsonAlias({"victorops_api_key"})
  private String victoropsApiKey;
  @JsonAlias({"victorops_api_url"})
  private String victoropsApiUrl;
  @JsonAlias({"pagerduty_url"})
  private String pagerdutyUrl;
  @JsonAlias({"opsgenie_api_key"})
  private String opsgenieApiKey;
  @JsonAlias({"opsgenie_api_url"})
  private String opsgenieApiUrl;
  @JsonAlias({"wechat_api_url"})
  private String wechatApiUrl;
  @JsonAlias({"wechat_api_secret"})
  private String wechatApiSecret;
  @JsonAlias({"wechat_api_corp_id"})
  private String wechatApiCorpId;
  //The default HTTP client configuration
  @JsonAlias({"http_config"})
  private HttpConfig httpConfig;
  //ResolveTimeout is the default value used by alertmanager if the alert does not include EndsAt
  @JsonAlias({"resolve_timeout"})
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

  public String getSmtpSmarthost() {
    return smtpSmarthost;
  }

  public void setSmtpSmarthost(String smtpSmarthost) {
    this.smtpSmarthost = smtpSmarthost;
  }

  public Global withSmtpSmarthost(String smtpSmarthost) {
    this.smtpSmarthost = smtpSmarthost;
    return this;
  }

  public String getSmtpFrom() {
    return smtpFrom;
  }

  public void setSmtpFrom(String smtpFrom) {
    this.smtpFrom = smtpFrom;
  }

  public Global withSmtpFrom(String smtpFrom) {
    this.smtpFrom = smtpFrom;
    return this;
  }

  public String getSmtpAuthUsername() {
    return smtpAuthUsername;
  }

  public void setSmtpAuthUsername(String smtpAuthUsername) {
    this.smtpAuthUsername = smtpAuthUsername;
  }

  public Global withSmtpAuthUsername(String smtpAuthUsername) {
    this.smtpAuthUsername = smtpAuthUsername;
    return this;
  }

  public String getSmtpAuthPassword() {
    return smtpAuthPassword;
  }

  public void setSmtpAuthPassword(String smtpAuthPassword) {
    this.smtpAuthPassword = smtpAuthPassword;
  }

  public Global withSmtpAuthPassword(String smtpAuthPassword) {
    this.smtpAuthPassword = smtpAuthPassword;
    return this;
  }

  public String getSmtpAuthIdentity() {
    return smtpAuthIdentity;
  }

  public void setSmtpAuthIdentity(String smtpAuthIdentity) {
    this.smtpAuthIdentity = smtpAuthIdentity;
  }

  public Global withSmtpAuthIdentity(String smtpAuthIdentity) {
    this.smtpAuthIdentity = smtpAuthIdentity;
    return this;
  }

  public String getSmtpAuthSecret() {
    return smtpAuthSecret;
  }

  public void setSmtpAuthSecret(String smtpAuthSecret) {
    this.smtpAuthSecret = smtpAuthSecret;
  }

  public Global withSmtpAuthSecret(String smtpAuthSecret) {
    this.smtpAuthSecret = smtpAuthSecret;
    return this;
  }

  public String getSmtpRequireTls() {
    return smtpRequireTls;
  }

  public void setSmtpRequireTls(String smtpRequireTls) {
    this.smtpRequireTls = smtpRequireTls;
  }

  public Global withSmtpRequireTls(String smtpRequireTls) {
    this.smtpRequireTls = smtpRequireTls;
    return this;
  }

  public String getSmtpHello() {
    return smtpHello;
  }

  public void setSmtpHello(String smtpHello) {
    this.smtpHello = smtpHello;
  }

  public Global withSmtpHello(String smtpHello) {
    this.smtpHello = smtpHello;
    return this;
  }

  public String getSlackApiUrl() {
    return slackApiUrl;
  }

  public void setSlackApiUrl(String slackApiUrl) {
    this.slackApiUrl = slackApiUrl;
  }

  public Global withSlackApiUrl(String slackApiUrl) {
    this.slackApiUrl = slackApiUrl;
    return this;
  }

  public String getVictoropsApiKey() {
    return victoropsApiKey;
  }

  public void setVictoropsApiKey(String victoropsApiKey) {
    this.victoropsApiKey = victoropsApiKey;
  }

  public Global withVictoropsApiKey(String victoropsApiKey) {
    this.victoropsApiKey = victoropsApiKey;
    return this;
  }

  public String getVictoropsApiUrl() {
    return victoropsApiUrl;
  }

  public void setVictoropsApiUrl(String victoropsApiUrl) {
    this.victoropsApiUrl = victoropsApiUrl;
  }

  public Global withVictoropsApiUrl(String victoropsApiUrl) {
    this.victoropsApiUrl = victoropsApiUrl;
    return this;
  }

  public String getPagerdutyUrl() {
    return pagerdutyUrl;
  }

  public void setPagerdutyUrl(String pagerdutyUrl) {
    this.pagerdutyUrl = pagerdutyUrl;
  }

  public Global withPagerdutyUrl(String pagerdutyUrl) {
    this.pagerdutyUrl = pagerdutyUrl;
    return this;
  }

  public String getOpsgenieApiKey() {
    return opsgenieApiKey;
  }

  public void setOpsgenieApiKey(String opsgenieApiKey) {
    this.opsgenieApiKey = opsgenieApiKey;
  }

  public Global withOpsgenieApiKey(String opsgenieApiKey) {
    this.opsgenieApiKey = opsgenieApiKey;
    return this;
  }

  public String getOpsgenieApiUrl() {
    return opsgenieApiUrl;
  }

  public void setOpsgenieApiUrl(String opsgenieApiUrl) {
    this.opsgenieApiUrl = opsgenieApiUrl;
  }

  public Global withOpsgenieApiUrl(String opsgenieApiUrl) {
    this.opsgenieApiUrl = opsgenieApiUrl;
    return this;
  }

  public String getWechatApiUrl() {
    return wechatApiUrl;
  }

  public void setWechatApiUrl(String wechatApiUrl) {
    this.wechatApiUrl = wechatApiUrl;
  }

  public Global withWechatApiUrl(String wechatApiUrl) {
    this.wechatApiUrl = wechatApiUrl;
    return this;
  }

  public String getWechatApiSecret() {
    return wechatApiSecret;
  }

  public void setWechatApiSecret(String wechatApiSecret) {
    this.wechatApiSecret = wechatApiSecret;
  }

  public Global withWechatApiSecret(String wechatApiSecret) {
    this.wechatApiSecret = wechatApiSecret;
    return this;
  }

  public String getWechatApiCorpId() {
    return wechatApiCorpId;
  }

  public void setWechatApiCorpId(String wechatApiCorpId) {
    this.wechatApiCorpId = wechatApiCorpId;
  }

  public Global withWechatApiCorpId(String wechatApiCorpId) {
    this.wechatApiCorpId = wechatApiCorpId;
    return this;
  }

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public void setHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  public Global withHttpConfig(HttpConfig httpConfig) {
    this.httpConfig = httpConfig;
    return this;
  }

  public String getResolveTimeout() {
    return resolveTimeout;
  }

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
