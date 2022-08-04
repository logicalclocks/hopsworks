/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation;

public enum FeatureGroupValidationStatus {
  NONE("None", 0),
  SUCCESS("Success", 1),
  WARNING("Warning", 2),
  FAILURE("Failure", 3);
  
  private final String name;
  private final int severity;
  
  FeatureGroupValidationStatus(String name, int severity) {
    this.name = name;
    this.severity = severity;
  }
  
  public int getSeverity() {
    return severity;
  }
  
  public static FeatureGroupValidationStatus fromString(String name) {
    return valueOf(name.toUpperCase());
  }
  
  public String getName() {
    return name;
  }
  
  @Override
  public String toString() {
    return name;
  }
}