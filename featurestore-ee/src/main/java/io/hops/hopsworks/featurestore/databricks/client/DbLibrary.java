/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.databricks.client;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class DbLibrary {

  @Getter @Setter
  private String jar;

  @Getter @Setter
  private DbPyPiLibrary pypi;

  public DbLibrary(DbPyPiLibrary pypi) {
    this.pypi = pypi;
  }

  public DbLibrary(String jar) {
    this.jar = jar;
  }
}
