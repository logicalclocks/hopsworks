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

package io.hops.hopsworks.common.dao.app;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FeatureJsonDTO {

  private String type;
  private String name;
  private String description;
  private Boolean primary;

  public FeatureJsonDTO() {
  }

  @XmlElement
  public String getType() {
    return type;
  }

  @XmlElement
  public String getName() {
    return name;
  }

  @XmlElement
  public String getDescription() {
    return description;
  }

  @XmlElement
  public Boolean getPrimary() {
    return primary;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  @Override
  public String toString() {
    return "FeatureJsonDTO{" +
        "type='" + type + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", primary=" + primary +
        '}';
  }
}
