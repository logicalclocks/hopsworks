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
package io.hops.hopsworks.persistence.entity.cloud;

public enum ProjectRoles {
  ALL("ALL", "ALL"),
  DATA_OWNER("DATA_OWNER", "Data owner"),
  DATA_SCIENTIST("DATA_SCIENTIST", "Data scientist");

  private final String name;
  private final String displayName;

  ProjectRoles(String name, String displayName) {
    this.name = name;
    this.displayName = displayName;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static ProjectRoles fromString(String name) {
    return valueOf(name.toUpperCase());
  }

  public static ProjectRoles fromDisplayName(String displayName) {
    switch (displayName) {
      case "ALL":
        return ProjectRoles.ALL;
      case "Data owner":
        return ProjectRoles.DATA_OWNER;
      case "Data scientist":
        return ProjectRoles.DATA_SCIENTIST;
      default:
        throw new IllegalArgumentException("No role found for given value.");
    }
  }
}
