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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import java.util.Objects;

public class Feature {
  private String name;
  private String fgAlias;
  private String pitFgAlias;
  private String type;
  private boolean primary;
  private String defaultValue;
  private String prefix;
  private Featuregroup featureGroup;
  private Integer idx;

  // For testing purposes
  public Feature(String name, String fgAlias, String type, boolean primary, String defaultValue, String prefix) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.type = type;
    this.primary = primary;
    this.defaultValue = defaultValue;
    this.prefix = prefix;
  }

  public Feature(String name, String fgAlias, String type, boolean primary, String defaultValue, String prefix,
      Featuregroup featuregroup) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.type = type;
    this.primary = primary;
    this.defaultValue = defaultValue;
    this.prefix = prefix;
    this.featureGroup = featuregroup;
  }

  public Feature(FeatureGroupFeatureDTO featureGroupFeatureDTO, Featuregroup featuregroup) {
    this.name = featureGroupFeatureDTO.getName();
    this.type = featureGroupFeatureDTO.getType();
    this.primary = featureGroupFeatureDTO.getPrimary();
    this.defaultValue = featureGroupFeatureDTO.getDefaultValue();
    this.featureGroup = featuregroup;
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

  // For testing purposes
  public Feature(String name, String fgAlias) {
    this.name = name;
    this.fgAlias = fgAlias;
  }
  
  // For testing purposes
  public Feature(String name, String fgAlias, Featuregroup featureGroup) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.featureGroup = featureGroup;
  }
  
  // For testing purposes
  public Feature(String name, String fgAlias, Featuregroup featureGroup, boolean primary) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.featureGroup = featureGroup;
    this.primary = primary;
  }

  // For testing purposes
  public Feature(String name, String fgAlias, Featuregroup featureGroup, String type, String defaultValue) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.featureGroup = featureGroup;
    this.type = type;
    this.defaultValue = defaultValue;
  }

  // For testing purposes
  public Feature(String name, String fgAlias, Featuregroup featureGroup, boolean primary, Integer idx) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.featureGroup = featureGroup;
    this.primary = primary;
    this.idx = idx;
  }

  public Feature(String name, String fgAlias, String type, String defaultValue, String prefix) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.type = type;
    this.defaultValue = defaultValue;
    this.prefix = prefix;
  }

  public Feature(String name, String fgAlias, String type, String defaultValue, boolean primary,
      Featuregroup featureGroup, String prefix) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.type = type;
    this.defaultValue = defaultValue;
    this.primary = primary;
    this.featureGroup = featureGroup;
    this.prefix = prefix;
  }

  public Feature(String name, String fgAlias, String type, String defaultValue, String prefix,
                 Featuregroup featureGroup, Integer idx) {
    this.name = name;
    this.fgAlias = fgAlias;
    this.type = type;
    this.defaultValue = defaultValue;
    this.prefix = prefix;
    this.featureGroup = featureGroup;
    this.idx = idx;
  }
  
  // for testing
  public Feature(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
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

  public String getFgAlias(boolean pitAlias) {
    if (pitAlias) {
      return pitFgAlias;
    }
    return fgAlias;
  }

  public void setFgAlias(String fgAlias) {
    this.fgAlias = fgAlias;
  }
  
  public void setPitFgAlias(String pitFgAlias) {
    this.pitFgAlias = pitFgAlias;
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

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }
  
  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }
  
  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  public Integer getIdx() {
    return idx;
  }
  
  public void setIdx(Integer idx) {
    this.idx = idx;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Feature feature = (Feature) o;
    return primary == feature.primary && Objects.equals(name, feature.name) &&
        Objects.equals(fgAlias, feature.fgAlias) && Objects.equals(pitFgAlias, feature.pitFgAlias) &&
        Objects.equals(type, feature.type) && Objects.equals(defaultValue, feature.defaultValue) &&
        Objects.equals(prefix, feature.prefix) &&
        Objects.equals(featureGroup, feature.featureGroup) && Objects.equals(idx, feature.idx);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, fgAlias, pitFgAlias, type, primary, defaultValue, prefix, featureGroup, idx);
  }
}
