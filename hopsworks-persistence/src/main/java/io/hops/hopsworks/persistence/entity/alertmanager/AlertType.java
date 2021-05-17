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
package io.hops.hopsworks.persistence.entity.alertmanager;

import javax.xml.bind.annotation.XmlEnum;
import java.util.HashMap;
import java.util.Map;

@XmlEnum
public enum AlertType {
  SYSTEM_ALERT( "SYSTEM_ALERT", "system-alert"),
  GLOBAL_ALERT_EMAIL( "GLOBAL_ALERT_EMAIL","global-alert-email"),
  GLOBAL_ALERT_SLACK( "GLOBAL_ALERT_SLACK","global-alert-slack"),
  GLOBAL_ALERT_PAGERDUTY( "GLOBAL_ALERT_PAGERDUTY","global-alert-pagerduty"),
  GLOBAL_ALERT_PUSHOVER( "GLOBAL_ALERT_PUSHOVER","global-alert-pushover"),
  GLOBAL_ALERT_OPSGENIE( "GLOBAL_ALERT_OPSGENIE","global-alert-opsgenie"),
  GLOBAL_ALERT_WEBHOOK( "GLOBAL_ALERT_WEBHOOK","global-alert-webhook"),
  GLOBAL_ALERT_VICTOROPS( "GLOBAL_ALERT_VICTOROPS","global-alert-victorops"),
  GLOBAL_ALERT_WEBCHAT( "GLOBAL_ALERT_WEBCHAT","global-alert-wechat"),
  PROJECT_ALERT( "PROJECT_ALERT","project-alert");

  private final String name;
  private final String value;

  private static final Map<String, AlertType> lookup = new HashMap<>();

  static {
    for (AlertType a : AlertType.values()) {
      lookup.put(a.value, a);
    }
  }

  AlertType(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public static AlertType fromString(String name) {
    return valueOf(name.toUpperCase());
  }

  public static AlertType fromValue(String value) {
    return lookup.get(value);
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public boolean isGlobal() {
    return !this.equals(AlertType.SYSTEM_ALERT) && !this.equals(AlertType.PROJECT_ALERT);
  }

}
