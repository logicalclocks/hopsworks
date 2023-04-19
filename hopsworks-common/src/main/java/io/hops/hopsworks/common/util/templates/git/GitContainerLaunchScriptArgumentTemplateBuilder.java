/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.util.templates.git;

public class GitContainerLaunchScriptArgumentTemplateBuilder {
  private String gitHome;
  private String hdfsUser;
  private String certificatesDir;
  private String imageName;
  private String commandLogFile;
  private String hopsfsMountLogFile;
  private String projectName;
  private String gitCommand;
  private String commandConfiguration;
  private String tokenPath;
  private String executionId;
  private String projectId;
  private String gitUsername;
  private String gitToken;
  private String repositoryId;

  private GitContainerLaunchScriptArgumentTemplateBuilder() {}

  public static GitContainerLaunchScriptArgumentTemplateBuilder newBuilder() {
    return new GitContainerLaunchScriptArgumentTemplateBuilder();
  }

  public String getGitHome() { return gitHome; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setGitHome(String gitHome) {
    this.gitHome = gitHome;
    return this;
  }

  public String getHdfsUser() { return hdfsUser; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setHdfsUser(String hdfsUser) {
    this.hdfsUser = hdfsUser;
    return this;
  }

  public String getCertificatesDir() { return certificatesDir; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setCertificatesDir(String certificatesDir) {
    this.certificatesDir = certificatesDir;
    return this;
  }

  public String getImageName() { return imageName; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setImageName(String imageName) {
    this.imageName = imageName;
    return this;
  }

  public String getCommandLogFile() { return commandLogFile; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setCommandLogFile(String commandLogFile) {
    this.commandLogFile = commandLogFile;
    return this;
  }

  public String getHopsfsMountLogFile() { return hopsfsMountLogFile; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setHopsfsMountLogFile(String hopsfsMountLogFile) {
    this.hopsfsMountLogFile = hopsfsMountLogFile;
    return this;
  }

  public String getProjectName() { return projectName; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setProjectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  public String getGitCommand() { return gitCommand; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setGitCommand(String gitCommand) {
    this.gitCommand = gitCommand;
    return this;
  }

  public String getCommandConfiguration() { return commandConfiguration; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setCommandConfiguration(String commandConfiguration) {
    this.commandConfiguration = commandConfiguration;
    return this;
  }

  public String getTokenPath() { return tokenPath; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setTokenPath(String tokenPath) {
    this.tokenPath = tokenPath;
    return this;
  }

  public String getExecutionId() { return executionId; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setExecutionId(String executionId) {
    this.executionId = executionId;
    return this;
  }

  public String getProjectId() { return projectId; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getGitUsername() { return gitUsername; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setGitUsername(String gitUsername) {
    this.gitUsername = gitUsername;
    return this;
  }

  public String getGitToken() { return gitToken; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setGitToken(String gitToken) {
    this.gitToken = gitToken;
    return this;
  }

  public String getRepositoryId() { return repositoryId; }

  public GitContainerLaunchScriptArgumentTemplateBuilder setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
    return this;
  }

  public GitContainerLaunchScriptArgumentsTemplate build() {
    return new GitContainerLaunchScriptArgumentsTemplate(this);
  }
}
