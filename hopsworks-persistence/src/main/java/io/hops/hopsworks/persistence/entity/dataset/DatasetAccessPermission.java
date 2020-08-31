/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.dataset;

import javax.xml.bind.annotation.XmlEnumValue;

public enum DatasetAccessPermission {
  EDITABLE("EDITABLE", "Editable by all"),
  READ_ONLY("READ_ONLY", "Read only"),
  EDITABLE_BY_OWNERS("EDITABLE_BY_OWNERS", "Editable by data owners");

  @XmlEnumValue(value = "value")
  private final String value;
  @XmlEnumValue(value = "description")
  private final String description;

  DatasetAccessPermission(String value, String description) {
    this.value = value;
    this.description = description;
  }

  public String getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }

  public static DatasetAccessPermission fromString(String value) {
    return valueOf(value.toUpperCase());
  }
}
