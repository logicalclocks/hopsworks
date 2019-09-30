/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.dao;

import java.util.Map;

public class JupyterGitConfigResponse extends JupyterGitResponse {
  private Map<String, String> options;
  
  public Map<String, String> getOptions() {
    return options;
  }
  
  public void setOptions(Map<String, String> options) {
    this.options = options;
  }
}
