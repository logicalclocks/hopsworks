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

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "name",
  "email_configs",
  "pagerduty_configs",
  "pushover_configs",
  "slack_configs",
  "opsgenie_configs",
  "webhook_configs",
  "victorops_configs",
  "wechat_configs"
  })
public class Receiver {

  @JsonProperty("name")
  private String name;
  @JsonProperty("email_configs")
  private List<EmailConfig> emailConfigs = null;
  @JsonProperty("pagerduty_configs")
  private List<PagerdutyConfig> pagerdutyConfigs = null;
  @JsonProperty("pushover_configs")
  private List<PushoverConfig> pushoverConfigs = null;
  @JsonProperty("slack_configs")
  private List<SlackConfig> slackConfigs = null;
  @JsonProperty("opsgenie_configs")
  private List<OpsgenieConfig> opsgenieConfigs = null;
  @JsonProperty("webhook_configs")
  private List<WebhookConfig> webhookConfigs = null;
  @JsonProperty("victorops_configs")
  private List<VictoropsConfig> victoropsConfigs = null;
  @JsonProperty("wechat_configs")
  private List<WechatConfig> wechatConfigs = null;

  public Receiver() {
  }

  /**
   * @param name
   */
  public Receiver(String name) {
    this.name = name;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("email_configs")
  public List<EmailConfig> getEmailConfigs() {
    return emailConfigs;
  }

  @JsonProperty("email_configs")
  public void setEmailConfigs(List<EmailConfig> emailConfigs) {
    this.emailConfigs = emailConfigs;
  }

  public Receiver withEmailConfigs(List<EmailConfig> emailConfigs) {
    this.emailConfigs = emailConfigs;
    return this;
  }

  @JsonProperty("pagerduty_configs")
  public List<PagerdutyConfig> getPagerdutyConfigs() {
    return pagerdutyConfigs;
  }

  @JsonProperty("pagerduty_configs")
  public void setPagerdutyConfigs(List<PagerdutyConfig> pagerdutyConfigs) {
    this.pagerdutyConfigs = pagerdutyConfigs;
  }

  public Receiver withPagerdutyConfigs(List<PagerdutyConfig> pagerdutyConfigs) {
    this.pagerdutyConfigs = pagerdutyConfigs;
    return this;
  }

  @JsonProperty("pushover_configs")
  public List<PushoverConfig> getPushoverConfigs() {
    return pushoverConfigs;
  }

  @JsonProperty("pushover_configs")
  public void setPushoverConfigs(List<PushoverConfig> pushoverConfigs) {
    this.pushoverConfigs = pushoverConfigs;
  }

  public Receiver withPushoverConfigs(List<PushoverConfig> pushoverConfigs) {
    this.pushoverConfigs = pushoverConfigs;
    return this;
  }

  @JsonProperty("slack_configs")
  public List<SlackConfig> getSlackConfigs() {
    return slackConfigs;
  }

  @JsonProperty("slack_configs")
  public void setSlackConfigs(List<SlackConfig> slackConfigs) {
    this.slackConfigs = slackConfigs;
  }

  public Receiver withSlackConfigs(List<SlackConfig> slackConfigs) {
    this.slackConfigs = slackConfigs;
    return this;
  }

  @JsonProperty("opsgenie_configs")
  public List<OpsgenieConfig> getOpsgenieConfigs() {
    return opsgenieConfigs;
  }

  @JsonProperty("opsgenie_configs")
  public void setOpsgenieConfigs(List<OpsgenieConfig> opsgenieConfigs) {
    this.opsgenieConfigs = opsgenieConfigs;
  }

  public Receiver withOpsgenieConfigs(List<OpsgenieConfig> opsgenieConfigs) {
    this.opsgenieConfigs = opsgenieConfigs;
    return this;
  }

  @JsonProperty("webhook_configs")
  public List<WebhookConfig> getWebhookConfigs() {
    return webhookConfigs;
  }

  @JsonProperty("webhook_configs")
  public void setWebhookConfigs(List<WebhookConfig> webhookConfigs) {
    this.webhookConfigs = webhookConfigs;
  }

  public Receiver withWebhookConfigs(List<WebhookConfig> webhookConfigs) {
    this.webhookConfigs = webhookConfigs;
    return this;
  }

  @JsonProperty("victorops_configs")
  public List<VictoropsConfig> getVictoropsConfigs() {
    return victoropsConfigs;
  }

  @JsonProperty("victorops_configs")
  public void setVictoropsConfigs(List<VictoropsConfig> victoropsConfigs) {
    this.victoropsConfigs = victoropsConfigs;
  }

  public Receiver withVictoropsConfigs(List<VictoropsConfig> victoropsConfigs) {
    this.victoropsConfigs = victoropsConfigs;
    return this;
  }

  @JsonProperty("wechat_configs")
  public List<WechatConfig> getWechatConfigs() {
    return wechatConfigs;
  }

  @JsonProperty("wechat_configs")
  public void setWechatConfigs(List<WechatConfig> wechatConfigs) {
    this.wechatConfigs = wechatConfigs;
  }

  public Receiver withWechatConfigs(List<WechatConfig> wechatConfigs) {
    this.wechatConfigs = wechatConfigs;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Receiver receiver = (Receiver) o;
    return name.equals(receiver.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "Receiver{" +
      "name='" + name + '\'' +
      ", emailConfigs=" + emailConfigs +
      ", pagerdutyConfigs=" + pagerdutyConfigs +
      ", pushoverConfigs=" + pushoverConfigs +
      ", slackConfigs=" + slackConfigs +
      ", opsgenieConfigs=" + opsgenieConfigs +
      ", webhookConfigs=" + webhookConfigs +
      ", victoropsConfigs=" + victoropsConfigs +
      ", wechatConfigs=" + wechatConfigs +
      '}';
  }
}
