/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.databricks.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class DbPyPiLibrary {

  @Getter @Setter
  @JsonProperty("package")
  private String pkg;

  public DbPyPiLibrary(String pkg) {
    this.pkg = pkg;
  }
}
