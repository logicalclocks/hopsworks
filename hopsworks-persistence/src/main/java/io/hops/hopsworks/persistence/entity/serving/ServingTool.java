/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Serving tools
 */

public enum ServingTool {
  // Docker container (Community)
  @JsonProperty("DEFAULT")
  DEFAULT;
  
  @JsonCreator
  public static ServingTool fromString(String servingTool) {
    if (servingTool != null) {
      switch (servingTool) {
        case "DEFAULT":
          return DEFAULT;
      }
    }
    return null;
  }
}
