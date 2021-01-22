/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.jwt.util;

import java.util.List;

public class Subject {
  private String name;
  private List<String> roles;
  
  public Subject(String name, List<String> roles) {
    this.name = name;
    this.roles = roles;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public List<String> getRoles() {
    return roles;
  }
  
  public void setRoles(List<String> roles) {
    this.roles = roles;
  }
}
