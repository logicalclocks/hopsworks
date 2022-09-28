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
package io.hops.hopsworks.api.alert.receiver;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class PostableReceiverDTO {
  private String name;
  private List<PostableEmailConfig> emailConfigs = null;
  private List<PostablePagerdutyConfig> pagerdutyConfigs = null;
  private List<PostablePushoverConfig> pushoverConfigs = null;
  private List<PostableSlackConfig> slackConfigs = null;
  private List<PostableOpsgenieConfig> opsgenieConfigs = null;
  private List<PostableWebhookConfig> webhookConfigs = null;
  private List<PostableVictoropsConfig> victoropsConfigs = null;
  private List<PostableWechatConfig> wechatConfigs = null;
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public List<PostableEmailConfig> getEmailConfigs() {
    return emailConfigs;
  }
  
  public void setEmailConfigs(List<PostableEmailConfig> emailConfigs) {
    this.emailConfigs = emailConfigs;
  }
  
  public List<PostablePagerdutyConfig> getPagerdutyConfigs() {
    return pagerdutyConfigs;
  }
  
  public void setPagerdutyConfigs(
      List<PostablePagerdutyConfig> pagerdutyConfigs) {
    this.pagerdutyConfigs = pagerdutyConfigs;
  }
  
  public List<PostablePushoverConfig> getPushoverConfigs() {
    return pushoverConfigs;
  }
  
  public void setPushoverConfigs(List<PostablePushoverConfig> pushoverConfigs) {
    this.pushoverConfigs = pushoverConfigs;
  }
  
  public List<PostableSlackConfig> getSlackConfigs() {
    return slackConfigs;
  }
  
  public void setSlackConfigs(List<PostableSlackConfig> slackConfigs) {
    this.slackConfigs = slackConfigs;
  }
  
  public List<PostableOpsgenieConfig> getOpsgenieConfigs() {
    return opsgenieConfigs;
  }
  
  public void setOpsgenieConfigs(List<PostableOpsgenieConfig> opsgenieConfigs) {
    this.opsgenieConfigs = opsgenieConfigs;
  }
  
  public List<PostableWebhookConfig> getWebhookConfigs() {
    return webhookConfigs;
  }
  
  public void setWebhookConfigs(List<PostableWebhookConfig> webhookConfigs) {
    this.webhookConfigs = webhookConfigs;
  }
  
  public List<PostableVictoropsConfig> getVictoropsConfigs() {
    return victoropsConfigs;
  }
  
  public void setVictoropsConfigs(List<PostableVictoropsConfig> victoropsConfigs) {
    this.victoropsConfigs = victoropsConfigs;
  }
  
  public List<PostableWechatConfig> getWechatConfigs() {
    return wechatConfigs;
  }
  
  public void setWechatConfigs(List<PostableWechatConfig> wechatConfigs) {
    this.wechatConfigs = wechatConfigs;
  }
}
