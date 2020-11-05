/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.integrations.databricks;

public class DatabricksApiKeyDTO {

  private String apiKey;

  public DatabricksApiKeyDTO() { }

  public DatabricksApiKeyDTO(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
