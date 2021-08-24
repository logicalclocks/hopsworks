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

@XmlRootElement
public class GlobalReceiverDefaults {
  private boolean emailConfigured;
  private boolean pagerdutyConfigured;
  private boolean pushoverConfigured;
  private boolean slackConfigured;
  private boolean opsgenieConfigured;
  private boolean webhookConfigured;
  private boolean victoropsConfigured;
  private boolean wechatConfigured;
  
  public GlobalReceiverDefaults() {
  }
  
  public boolean isEmailConfigured() {
    return emailConfigured;
  }
  
  public void setEmailConfigured(boolean emailConfigured) {
    this.emailConfigured = emailConfigured;
  }
  
  public boolean isPagerdutyConfigured() {
    return pagerdutyConfigured;
  }
  
  public void setPagerdutyConfigured(boolean pagerdutyConfigured) {
    this.pagerdutyConfigured = pagerdutyConfigured;
  }
  
  public boolean isPushoverConfigured() {
    return pushoverConfigured;
  }
  
  public void setPushoverConfigured(boolean pushoverConfigured) {
    this.pushoverConfigured = pushoverConfigured;
  }
  
  public boolean isSlackConfigured() {
    return slackConfigured;
  }
  
  public void setSlackConfigured(boolean slackConfigured) {
    this.slackConfigured = slackConfigured;
  }
  
  public boolean isOpsgenieConfigured() {
    return opsgenieConfigured;
  }
  
  public void setOpsgenieConfigured(boolean opsgenieConfigured) {
    this.opsgenieConfigured = opsgenieConfigured;
  }
  
  public boolean isWebhookConfigured() {
    return webhookConfigured;
  }
  
  public void setWebhookConfigured(boolean webhookConfigured) {
    this.webhookConfigured = webhookConfigured;
  }
  
  public boolean isVictoropsConfigured() {
    return victoropsConfigured;
  }
  
  public void setVictoropsConfigured(boolean victoropsConfigured) {
    this.victoropsConfigured = victoropsConfigured;
  }
  
  public boolean isWechatConfigured() {
    return wechatConfigured;
  }
  
  public void setWechatConfigured(boolean wechatConfigured) {
    this.wechatConfigured = wechatConfigured;
  }
}
