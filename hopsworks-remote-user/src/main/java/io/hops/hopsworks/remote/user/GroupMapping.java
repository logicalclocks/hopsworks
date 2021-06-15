/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

public enum GroupMapping {
  ANY_TO_USER("ANY_TO_USER", "ANY_GROUP -> HOPS_USER"),
  ANY_TO_ADMIN("ANY_TO_ADMIN","ANY_GROUP -> HOPS_ADMIN"),
  GROUP_MAPPING("GROUP_MAPPING", "");
  
  private final String name;
  private final String value;
  
  GroupMapping(String name, String value) {
    this.name = name;
    this.value = value;
  }
  
  public String getName() {
    return name;
  }
  
  public String getValue() {
    return value;
  }
}
