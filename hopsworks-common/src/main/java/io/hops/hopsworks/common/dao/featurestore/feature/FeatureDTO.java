/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.feature;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO containing the human-readable information of a feature, can be converted to JSON or XML representation
 * using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"name", "type", "description", "primary", "partition", "onlineType"})
public class FeatureDTO {

  private String name;
  private String type;
  private String onlineType;
  private String description;
  private Boolean primary = false;
  private Boolean partition = false;

  public FeatureDTO(){}

  public FeatureDTO(String name, String type, String description, Boolean primary, Boolean partition,
    String onlineType) {
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
    this.partition = partition;
    this.onlineType = onlineType;
  }

  public FeatureDTO(String name, String type, String description) {
    this.name = name;
    this.type = type;
    this.description = description;
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
  
  @Override
  public String toString() {
    return "FeatureDTO{" +
      "name='" + name + '\'' +
      ", type='" + type + '\'' +
      ", onlineType='" + onlineType + '\'' +
      ", description='" + description + '\'' +
      ", primary=" + primary +
      ", partition=" + partition +
      '}';
  }
}
