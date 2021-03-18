/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;

public class Feature {
  private String name;
  private String fgAlias;
  private String type;
  private boolean primary;
  private String defaultValue;

  public Feature(String name, String fgAlias, String type, boolean primary, String defaultValue) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.type = type;
    this.primary = primary;
    this.defaultValue = defaultValue;
  }
  
  public Feature(FeatureGroupFeatureDTO featureGroupFeatureDTO, String fgAlias) {
    this.name = featureGroupFeatureDTO.getName();
    this.fgAlias = fgAlias;
    this.type = featureGroupFeatureDTO.getType();
    this.primary = featureGroupFeatureDTO.getPrimary();
    this.defaultValue = featureGroupFeatureDTO.getDefaultValue();
  }

  // For testing purposes
  public Feature(String name, String fgAlias, boolean primary) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.primary = primary;
  }

  public Feature(String name, String fgAlias, String type, String defaultValue) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.type = type;
    this.defaultValue = defaultValue;
  }

  // For testing purposes only
  public Feature(String name, boolean primary) {
    this.name = name;
    this.primary = primary;
  }

  public Feature(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFgAlias() {
    return fgAlias;
  }

  public void setFgAlias(String fgAlias) {
    this.fgAlias = fgAlias;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isPrimary() {
    return primary;
  }

  public void setPrimary(boolean primary) {
    this.primary = primary;
  }
  
  public String getDefaultValue() {
    return defaultValue;
  }
  
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }
}
