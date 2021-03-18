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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information of a feature, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
public class FeatureGroupFeatureDTO {

  private String name;
  private String type;
  private String onlineType;
  private String description;
  private Boolean primary = false;
  private Boolean partition = false;
  private Boolean hudiPrecombineKey  = false;
  private String defaultValue = null;
  private Integer featureGroupId = null;

  public FeatureGroupFeatureDTO(){}

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

  public FeatureGroupFeatureDTO(String name, String type, String description, Integer featureGroupId) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.featureGroupId = featureGroupId;
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

  @XmlElement
  public String getName() {
    return name;
  }

  @XmlElement
  public String getType() {
    return type;
  }

  @XmlElement
  public String getDescription() {
    return description;
  }

  @XmlElement
  public Boolean getPrimary() {
    return primary;
  }

  @XmlElement
  public Boolean getPartition() {
    return partition;
  }

  @XmlElement
  public Boolean getHudiPrecombineKey() {
    return hudiPrecombineKey;
  }

  @XmlElement
  public String getOnlineType() {
    return onlineType;
  }
  
  @XmlElement(nillable = true)
  public String getDefaultValue() {
    return defaultValue;
  }

  @XmlElement
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
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + (onlineType != null ? onlineType.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + primary.hashCode();
    result = 31 * result + partition.hashCode();
    result = 31 * result + hudiPrecombineKey.hashCode();
    result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
    result = 31 * result + (featureGroupId != null ? featureGroupId.hashCode() : 0);
    return result;
  }
}
