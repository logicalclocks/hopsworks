/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.databricks.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
public class DbLibraryInstall {
  @Getter @Setter
  @JsonProperty("cluster_id")
  private String clusterId;

  @Getter @Setter
  private List<DbLibrary> libraries;

  public DbLibraryInstall(String clusterId) {
    this.clusterId = clusterId;
  }
}
