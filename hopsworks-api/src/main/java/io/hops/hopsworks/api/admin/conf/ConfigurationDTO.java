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

package io.hops.hopsworks.api.admin.conf;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.util.VariablesVisibility;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConfigurationDTO extends RestDTO<ConfigurationDTO> {

  private String name;
  private String value;
  private VariablesVisibility visibility = VariablesVisibility.ADMIN;
  private Boolean hide = false;

  public ConfigurationDTO() {}

  public ConfigurationDTO(String name, String value,
                          VariablesVisibility visibility, Boolean hide) {
    this.name = name;
    this.value = value;
    this.visibility = visibility;
    this.hide = hide;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public VariablesVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(VariablesVisibility visibility) {
    this.visibility = visibility;
  }

  public Boolean getHide() {
    return hide;
  }

  public void setHide(Boolean hide) {
    this.hide = hide;
  }

  @Override
  public String toString() {
    return "ConfigurationDTO{" +
        "name='" + name + '\'' +
        ", value='" + value + '\'' +
        ", visibility=" + visibility +
        ", hide=" + hide +
        '}';
  }
}
