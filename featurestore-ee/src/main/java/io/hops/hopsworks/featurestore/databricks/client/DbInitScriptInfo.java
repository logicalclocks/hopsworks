/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.databricks.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class DbInitScriptInfo {

  @Getter @Setter
  private DbfsStorageInfo dbfs;

  @Getter @Setter
  private DbfsStorageInfo workspace;

  @Getter @Setter
  private DbfsStorageInfo volumes;

  @Getter @Setter
  private DbfsStorageInfo file;
}
