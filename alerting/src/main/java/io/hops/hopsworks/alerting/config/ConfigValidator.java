/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.alerting.config;

import com.google.common.base.Strings;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.Global;
import io.hops.hopsworks.alerting.config.dto.InhibitRule;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;

import java.util.List;

public class ConfigValidator {
  
  //private constructor to hide the implicit public one.
  private ConfigValidator() {
  }
  
  public static void validate(Global global) {
    if (global == null) {
      throw new IllegalArgumentException("Global config can not be null.");
    }
  }
  
  public static void validate(List<String> templates) {
    if (templates == null || templates.isEmpty()) {
      throw new IllegalArgumentException("Templates config can not be null or empty.");
    }
  }
  
  public static void validateGlobalRoute(Route route) {
    if (route == null) {
      throw new IllegalArgumentException("Route config can not be null.");
    }
  }
  
  public static void validateInhibitRules(List<InhibitRule> inhibitRules) {
    if (inhibitRules == null || inhibitRules.isEmpty()) {
      throw new IllegalArgumentException("Inhibit Rules config can not be null or empty.");
    }
  }
  
  public static void validate(Receiver receiver) {
    if (Strings.isNullOrEmpty(receiver.getName())) {
      throw new IllegalArgumentException("Receiver name can not be empty.");
    }
    if ((receiver.getEmailConfigs() == null || receiver.getEmailConfigs().isEmpty()) &&
      (receiver.getSlackConfigs() == null || receiver.getSlackConfigs().isEmpty()) &&
      (receiver.getOpsgenieConfigs() == null || receiver.getOpsgenieConfigs().isEmpty()) &&
      (receiver.getPagerdutyConfigs() == null || receiver.getPagerdutyConfigs().isEmpty()) &&
      (receiver.getPushoverConfigs() == null || receiver.getPushoverConfigs().isEmpty()) &&
      (receiver.getVictoropsConfigs() == null || receiver.getVictoropsConfigs().isEmpty()) &&
      (receiver.getWebhookConfigs() == null || receiver.getWebhookConfigs().isEmpty()) &&
      (receiver.getWechatConfigs() == null || receiver.getWechatConfigs().isEmpty())) {
      throw new IllegalArgumentException("Receiver needs at least one configuration.");
    }
  }
  
  public static void validate(EmailConfig emailConfig) {
    if (Strings.isNullOrEmpty(emailConfig.getTo())) {
      throw new IllegalArgumentException("EmailConfig to can not be empty.");
    }
  }
  
  public static void validate(SlackConfig slackConfig) {
    if (Strings.isNullOrEmpty(slackConfig.getApiUrl()) || Strings.isNullOrEmpty(slackConfig.getChannel())) {
      throw new IllegalArgumentException("SlackConfig api url and channel can not be empty.");
    }
  }
  
  public static void validate(PagerdutyConfig pagerdutyConfig) {
    if (!Strings.isNullOrEmpty(pagerdutyConfig.getRoutingKey()) &&
      !Strings.isNullOrEmpty(pagerdutyConfig.getServiceKey())) {
      throw new IllegalArgumentException("PagerdutyConfig RoutingKey or ServiceKey must be set.");
    }
  }
  
  public static void validate(Route route) {
    if (Strings.isNullOrEmpty(route.getReceiver())) {
      throw new IllegalArgumentException("Route receiver can not be empty.");
    }
    if ((route.getMatch() == null || route.getMatch().isEmpty()) &&
      (route.getMatchRe() == null || route.getMatchRe().isEmpty())) {
      throw new IllegalArgumentException("Route needs to set match or match regex.");
    }
  }
}
