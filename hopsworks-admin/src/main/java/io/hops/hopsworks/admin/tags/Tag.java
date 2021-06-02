/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.admin.tags;

import java.io.Serializable;

public class Tag implements Serializable {
  private String name;
  private String schema;
  
  public Tag() {
  }
  
  public Tag(String name, String schema) {
    this.name = name;
    this.schema = schema;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getSchema() {
    return schema;
  }
  
  public void setSchema(String schema) {
    this.schema = schema;
  }
}
