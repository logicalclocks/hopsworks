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

package io.hops.hopsworks.common.featurestore.feature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

/**
 * DTO containing the human-readable information of a feature, can be converted to JSON or XML representation
 * using jaxb.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureGroupFeatureDTO {

  private String name;
  private String type;
  private String onlineType;
  private String description;
  @JsonSetter(nulls = Nulls.SKIP)
  private Boolean primary = false;
  @JsonSetter(nulls = Nulls.SKIP)
  private Boolean partition = false;
  @JsonSetter(nulls = Nulls.SKIP)
  private Boolean hudiPrecombineKey = false;
  private String defaultValue = null;
  private Integer featureGroupId = null;

  public FeatureGroupFeatureDTO(){}

  // for testing
  public FeatureGroupFeatureDTO(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public FeatureGroupFeatureDTO(String name, String type, boolean partition, String defaultValue) {
    this.name = name;
    this.type = type;
    this.partition = partition;
    this.defaultValue = defaultValue;
  }

  public FeatureGroupFeatureDTO(String name, String type, String description, Boolean primary,
                                Boolean partition, String onlineType, String defaultValue, Integer featureGroupId) {
    this.name = name;
    this.type = type;
    this.onlineType = onlineType;
    this.description = description;
    this.primary = primary;
    this.partition = partition;
    this.defaultValue = defaultValue;
    this.featureGroupId = featureGroupId;
  }

  public FeatureGroupFeatureDTO(String name, String type, String description, Boolean primary, String defaultValue,
                                Integer featureGroupId) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
    this.defaultValue = defaultValue;
    this.featureGroupId = featureGroupId;
  }

  // for testing
  public FeatureGroupFeatureDTO(String name, String type, String description, Boolean primary, String defaultValue) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
    this.defaultValue = defaultValue;
  }
  
  public FeatureGroupFeatureDTO(String name, String type, String description, Boolean primary, Boolean partition) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
    this.partition = partition;
  }

  public FeatureGroupFeatureDTO(String name, String type, String description, Boolean primary, Boolean partition,
                                String defaultValue, Integer featureGroupId) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
    this.partition = partition;
    this.defaultValue = defaultValue;
    this.featureGroupId = featureGroupId;
  }

  public FeatureGroupFeatureDTO(String name, String type, String description, Boolean primary, Boolean partition,
                                Boolean hudiPrecombineKey, String defaultValue, Integer featureGroupId) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
    this.partition = partition;
    this.hudiPrecombineKey = hudiPrecombineKey;
    this.defaultValue = defaultValue;
    this.featureGroupId = featureGroupId;
  }

  public FeatureGroupFeatureDTO(String name, String type, String description) {
    this.name = name;
    this.type = type;
    this.description = description;
  }

  public FeatureGroupFeatureDTO(String name, String type, String description, Integer featureGroupId, Boolean primary) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.featureGroupId = featureGroupId;
    this.primary = primary;
  }

  public FeatureGroupFeatureDTO(String name, String type, Boolean primary, String defaultValue,
                                Integer featureGroupId) {
    this.name = name;
    this.type = type;
    this.primary = primary;
    this.defaultValue = defaultValue;
    this.featureGroupId = featureGroupId;
  }

  public FeatureGroupFeatureDTO(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public Boolean getPrimary() {
    return primary;
  }

  public Boolean getPartition() {
    return partition;
  }

  public Boolean getHudiPrecombineKey() {
    return hudiPrecombineKey;
  }

  public String getOnlineType() {
    return onlineType;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Integer getFeatureGroupId() {
    return featureGroupId;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setPartition(Boolean partition) {
    this.partition = partition;
  }

  public void setHudiPrecombineKey(Boolean hudiPrecombineKey) {
    this.hudiPrecombineKey = hudiPrecombineKey;
  }

  public void setOnlineType(String onlineType) {
    this.onlineType = onlineType;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }
  
  public void setFeatureGroupId(Integer featureGroupId) {
    this.featureGroupId = featureGroupId;
  }

  @Override
  public String toString() {
    return "FeatureDTO{" +
      "name='" + name + '\'' +
      ", type='" + type + '\'' +
      ", onlineType='" + onlineType + '\'' +
      ", description='" + description + '\'' +
      ", primary=" + primary + '\'' +
      ", partition=" + partition + '\'' +
      ", defaultValue=" + defaultValue + '\'' +
      ", featureGroupName=" + featureGroupId +
      ", hudiPrecombineKey=" + hudiPrecombineKey + '\'' +
      ", defaultValue=" + defaultValue +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FeatureGroupFeatureDTO that = (FeatureGroupFeatureDTO) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (onlineType != null ? !onlineType.equals(that.onlineType) : that.onlineType != null) return false;
    if (description != null ? !description.equals(that.description) : that.description != null) return false;
    if (primary != null ? !primary.equals(that.primary) : that.primary != null) return false;
    if (partition != null ? !partition.equals(that.partition) : that.partition != null) return false;
    if (hudiPrecombineKey != null ? !hudiPrecombineKey.equals(that.hudiPrecombineKey) : that.hudiPrecombineKey != null)
      return false;
    if (defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null) return false;
    return featureGroupId != null ? featureGroupId.equals(that.featureGroupId) : that.featureGroupId == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (onlineType != null ? onlineType.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (primary != null ? primary.hashCode() : 0);
    result = 31 * result + (partition != null ? partition.hashCode() : 0);
    result = 31 * result + (hudiPrecombineKey != null ? hudiPrecombineKey.hashCode() : 0);
    result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
    result = 31 * result + (featureGroupId != null ? featureGroupId.hashCode() : 0);
    return result;
  }
}
