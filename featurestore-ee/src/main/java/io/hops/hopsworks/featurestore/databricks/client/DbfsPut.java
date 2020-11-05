/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.databricks.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class DbfsPut {

  @Getter @Setter
  private String path;

  @Getter @Setter
  private byte[] contents;

  @Getter @Setter
  private Boolean overwrite;
}
