/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
