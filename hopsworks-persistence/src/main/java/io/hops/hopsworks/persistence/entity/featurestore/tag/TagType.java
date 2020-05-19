/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.persistence.entity.featurestore.tag;

public enum TagType {
  STRING("String"),
  //More types
  //Need to fix index types for tags (cascade) when deleting and editing!!!!!
  ;
  TagType(String string) {
  
  }
}
