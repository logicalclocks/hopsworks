/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.api.python.environment.history;

import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.python.history.LibrarySpec;
import io.hops.hopsworks.persistence.entity.python.history.LibraryUpdate;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class EnvironmentHistoryDTO extends RestDTO<EnvironmentHistoryDTO> {
  private Integer id;
  private String environmentName;
  private String previousEnvironment;
  private String yamlFilePath;
  private String customCommandsFile;
  private List<LibrarySpec> installed;
  private List<LibrarySpec> uninstalled;
  private List<LibraryUpdate> upgraded;
  private List<LibraryUpdate> downgraded;
  private Set<LibrarySpec> installedLibraries;
  private UserDTO user;
  private Date dateCreated;

  public Integer getId() { return id; }

  public void setId(Integer id) { this.id = id; }

  public String getEnvironmentName() { return environmentName; }

  public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }

  public String getPreviousEnvironment() { return previousEnvironment; }

  public void setPreviousEnvironment(String previousEnvironment) { this.previousEnvironment = previousEnvironment; }

  public String getYamlFilePath() { return yamlFilePath; }

  public void setYamlFilePath(String yamlFilePath) { this.yamlFilePath = yamlFilePath; }

  public String getCustomCommandsFile() { return customCommandsFile; }

  public void setCustomCommandsFile(String customCommandsFile) { this.customCommandsFile = customCommandsFile; }

  public List<LibrarySpec> getInstalled() { return installed; }

  public void setInstalled(List<LibrarySpec> installed) { this.installed = installed; }

  public List<LibrarySpec> getUninstalled() { return uninstalled; }

  public void setUninstalled(List<LibrarySpec> uninstalled) { this.uninstalled = uninstalled; }

  public List<LibraryUpdate> getUpgraded() { return upgraded; }

  public void setUpgraded(List<LibraryUpdate> upgraded) { this.upgraded = upgraded; }

  public List<LibraryUpdate> getDowngraded() { return downgraded; }

  public void setDowngraded(List<LibraryUpdate> downgraded) { this.downgraded = downgraded; }

  public UserDTO getUser() { return user; }

  public void setUser(UserDTO user) { this.user = user; }

  public Set<LibrarySpec> getInstalledLibraries() { return installedLibraries; }

  public void setInstalledLibraries(Set<LibrarySpec> installedLibraries) {
    this.installedLibraries = installedLibraries;
  }

  public Date getDateCreated() { return dateCreated; }

  public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
}
