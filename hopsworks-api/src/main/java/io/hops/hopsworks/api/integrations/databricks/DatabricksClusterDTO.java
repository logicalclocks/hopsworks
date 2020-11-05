/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.integrations.databricks;

import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class DatabricksClusterDTO extends RestDTO<DatabricksClusterDTO> {

  private String id;
  private String name;
  private String state;
  private Map<String, String> sparkConfiguration;
  private String project;
  private UserDTO user;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Map<String, String> getSparkConfiguration() {
    return sparkConfiguration;
  }

  public void setSparkConfiguration(Map<String, String> sparkConfiguration) {
    this.sparkConfiguration = sparkConfiguration;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public UserDTO getUser() {
    return user;
  }

  public void setUser(UserDTO user) {
    this.user = user;
  }
}
