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

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Receiver {

  private String name;
  @JsonAlias({"email_configs"})
  private List<EmailConfig> emailConfigs = null;
  @JsonAlias({"pagerduty_configs"})
  private List<PagerdutyConfig> pagerdutyConfigs = null;
  @JsonAlias({"pushover_configs"})
  private List<PushoverConfig> pushoverConfigs = null;
  @JsonAlias({"slack_configs"})
  private List<SlackConfig> slackConfigs = null;
  @JsonAlias({"opsgenie_configs"})
  private List<OpsgenieConfig> opsgenieConfigs = null;
  @JsonAlias({"webhook_configs"})
  private List<WebhookConfig> webhookConfigs = null;
  @JsonAlias({"victorops_configs"})
  private List<VictoropsConfig> victoropsConfigs = null;
  @JsonAlias({"wechat_configs"})
  private List<WechatConfig> wechatConfigs = null;

  public Receiver() {
  }

  /**
   * @param name
   */
  public Receiver(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<EmailConfig> getEmailConfigs() {
    return emailConfigs;
  }

  public void setEmailConfigs(List<EmailConfig> emailConfigs) {
    this.emailConfigs = emailConfigs;
  }

  public Receiver withEmailConfigs(List<EmailConfig> emailConfigs) {
    this.emailConfigs = emailConfigs;
    return this;
  }

  public List<PagerdutyConfig> getPagerdutyConfigs() {
    return pagerdutyConfigs;
  }

  public void setPagerdutyConfigs(List<PagerdutyConfig> pagerdutyConfigs) {
    this.pagerdutyConfigs = pagerdutyConfigs;
  }

  public Receiver withPagerdutyConfigs(List<PagerdutyConfig> pagerdutyConfigs) {
    this.pagerdutyConfigs = pagerdutyConfigs;
    return this;
  }

  public List<PushoverConfig> getPushoverConfigs() {
    return pushoverConfigs;
  }

  public void setPushoverConfigs(List<PushoverConfig> pushoverConfigs) {
    this.pushoverConfigs = pushoverConfigs;
  }

  public Receiver withPushoverConfigs(List<PushoverConfig> pushoverConfigs) {
    this.pushoverConfigs = pushoverConfigs;
    return this;
  }

  public List<SlackConfig> getSlackConfigs() {
    return slackConfigs;
  }

  public void setSlackConfigs(List<SlackConfig> slackConfigs) {
    this.slackConfigs = slackConfigs;
  }

  public Receiver withSlackConfigs(List<SlackConfig> slackConfigs) {
    this.slackConfigs = slackConfigs;
    return this;
  }

  public List<OpsgenieConfig> getOpsgenieConfigs() {
    return opsgenieConfigs;
  }

  public void setOpsgenieConfigs(List<OpsgenieConfig> opsgenieConfigs) {
    this.opsgenieConfigs = opsgenieConfigs;
  }

  public Receiver withOpsgenieConfigs(List<OpsgenieConfig> opsgenieConfigs) {
    this.opsgenieConfigs = opsgenieConfigs;
    return this;
  }

  public List<WebhookConfig> getWebhookConfigs() {
    return webhookConfigs;
  }

  public void setWebhookConfigs(List<WebhookConfig> webhookConfigs) {
    this.webhookConfigs = webhookConfigs;
  }

  public Receiver withWebhookConfigs(List<WebhookConfig> webhookConfigs) {
    this.webhookConfigs = webhookConfigs;
    return this;
  }

  public List<VictoropsConfig> getVictoropsConfigs() {
    return victoropsConfigs;
  }

  public void setVictoropsConfigs(List<VictoropsConfig> victoropsConfigs) {
    this.victoropsConfigs = victoropsConfigs;
  }

  public Receiver withVictoropsConfigs(List<VictoropsConfig> victoropsConfigs) {
    this.victoropsConfigs = victoropsConfigs;
    return this;
  }

  public List<WechatConfig> getWechatConfigs() {
    return wechatConfigs;
  }

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
