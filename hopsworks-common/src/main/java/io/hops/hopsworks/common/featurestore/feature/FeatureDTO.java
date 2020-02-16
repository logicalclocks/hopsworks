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
import javax.xml.bind.annotation.XmlType;

/**
 * DTO containing the human-readable information of a feature, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"name", "type", "description", "primary", "partition", "onlineType", "featuregroup", "version"})
public class FeatureDTO {

  private String name;
  private String type;
  private String onlineType;
  private String description;
  private Boolean primary = false;
  private Boolean partition = false;
  private String featuregroup = null;
  private Integer version = null;

  public FeatureDTO(){}
  
  public FeatureDTO(String name, String type, String onlineType, String description, Boolean primary,
    Boolean partition, String featuregroup, Integer version) {
    this.name = name;
    this.type = type;
    this.onlineType = onlineType;
    this.description = description;
    this.primary = primary;
    this.partition = partition;
    this.featuregroup = featuregroup;
    this.version = version;
  }

  public FeatureDTO(String name, String type, String description, Boolean primary,
                    Boolean partition, String onlineType) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
    this.partition = partition;
    this.onlineType = onlineType;
  }

  public FeatureDTO(String name, String type, String description, Boolean primary) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
  }

  public FeatureDTO(String name, String type, String description, String featuregroup, Boolean primary) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.featuregroup = featuregroup;
    this.primary = primary;
  }

  public FeatureDTO(String name, String type, String description) {
    this.name = name;
    this.type = type;
    this.description = description;
  }

  public FeatureDTO(String name, String type, Boolean primary) {
    this.name = name;
    this.type = type;
    this.primary = primary;
  }

  public FeatureDTO(String name) {
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
  public String getOnlineType() {
    return onlineType;
  }
  
  @XmlElement
  public String getFeaturegroup() {
    return featuregroup;
  }
  
  @XmlElement
  public Integer getVersion() {
    return version;
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
  
  public void setOnlineType(String onlineType) {
    this.onlineType = onlineType;
  }
  
  public void setFeaturegroup(String featuregroup) {
    this.featuregroup = featuregroup;
  }
  
  public void setVersion(Integer version) {
    this.version = version;
  }
  
  @Override
  public String toString() {
    return "FeatureDTO{" +
      "name='" + name + '\'' +
      ", type='" + type + '\'' +
      ", onlineType='" + onlineType + '\'' +
      ", description='" + description + '\'' +
      ", primary=" + primary +
      ", partition=" + partition +
      ", featuregroup='" + featuregroup + '\'' +
      ", version=" + version +
      '}';
  }
}
