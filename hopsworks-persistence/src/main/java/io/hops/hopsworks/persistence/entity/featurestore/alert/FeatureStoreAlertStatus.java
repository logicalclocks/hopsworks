/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.featurestore.alert;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum FeatureStoreAlertStatus {
  SUCCESS("Success"),
  WARNING("Warning"),
  FAILURE("Failure"),
  FEATURE_MONITOR_SHIFT_UNDETECTED("FEATURE_MONITOR_SHIFT_UNDETECTED"),
  FEATURE_MONITOR_SHIFT_DETECTED("FEATURE_MONITOR_SHIFT_DETECTED");
  
  private final String name;
  
  FeatureStoreAlertStatus(String name) {
    this.name = name;
  }
  
  public static FeatureStoreAlertStatus fromString(String name) {
    return valueOf(name.toUpperCase());
  }

  public String getName() {
    return name;
  }
  
  public static FeatureStoreAlertStatus fromBooleanFeatureMonitorResultStatus(Boolean status) {
    if (status)
      return FEATURE_MONITOR_SHIFT_DETECTED;
    else
      return FEATURE_MONITOR_SHIFT_UNDETECTED;
  }
  
  public static boolean isFeatureMonitoringStatus(FeatureStoreAlertStatus status) {
    switch (status) {
      case FEATURE_MONITOR_SHIFT_DETECTED:
      case FEATURE_MONITOR_SHIFT_UNDETECTED:
        return Boolean.TRUE;
      default:
        return Boolean.FALSE;
    }
  }

  @Override
  public String toString() {
    return name;
  }
}
