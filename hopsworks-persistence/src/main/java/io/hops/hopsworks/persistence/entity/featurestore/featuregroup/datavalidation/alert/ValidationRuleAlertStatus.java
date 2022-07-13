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
package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidationStatus;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum ValidationRuleAlertStatus {
  SUCCESS("Success"),
  WARNING("Warning"),
  FAILURE("Failure");
  
  private final String name;
  
  ValidationRuleAlertStatus(String name) {
    this.name = name;
  }
  
  public static ValidationRuleAlertStatus fromString(String name) {
    return valueOf(name.toUpperCase());
  }
  
  public String getName() {
    return name;
  }
  
  public static ValidationRuleAlertStatus getStatus(FeatureGroupValidationStatus status) {
    switch (status) {
      case FAILURE:
        return ValidationRuleAlertStatus.FAILURE;
      case SUCCESS:
        return ValidationRuleAlertStatus.SUCCESS;
      case WARNING:
        return ValidationRuleAlertStatus.WARNING;
      default:
        throw new IllegalArgumentException("Invalid enum constant");//will happen if status is none
    }
  }
  
  @Override
  public String toString() {
    return name;
  }
}
