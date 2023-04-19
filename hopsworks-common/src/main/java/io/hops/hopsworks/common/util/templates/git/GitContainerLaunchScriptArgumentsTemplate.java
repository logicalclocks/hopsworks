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

public class GitContainerLaunchScriptArgumentsTemplate {
  public static final String TEMPLATE_NAME = "git_container_launch_script_arguments_template";
  public static final String FILE_NAME = "git_container_launch_script_arguments";

  private final String gitHome;
  private final String hdfsUser;
  private final String certificatesDir;
  private final String imageName;
  private final String commandLogFile;
  private final String hopsfsMountLogFile;
  private final String projectName;
  private final String gitCommand;
  private final String commandConfiguration;
  private final String tokenPath;
  private final String executionId;
  private final String projectId;
  private final String gitUsername;
  private final String gitToken;
  private final String repositoryId;

  public GitContainerLaunchScriptArgumentsTemplate(GitContainerLaunchScriptArgumentTemplateBuilder builder) {
    this.gitHome = builder.getGitHome();
    this.hdfsUser = builder.getHdfsUser();
    this.certificatesDir = builder.getCertificatesDir();
    this.imageName = builder.getImageName();
    this.commandLogFile = builder.getCommandLogFile();
    this.hopsfsMountLogFile = builder.getHopsfsMountLogFile();
    this.projectName = builder.getProjectName();
    this.gitCommand = builder.getGitCommand();
    this.commandConfiguration = builder.getCommandConfiguration();
    this.tokenPath = builder.getTokenPath();
    this.executionId = builder.getExecutionId();
    this.projectId = builder.getProjectId();
    this.gitUsername = builder.getGitUsername();
    this.gitToken = builder.getGitToken();
    this.repositoryId = builder.getRepositoryId();
  }

  public String getGitHome() { return gitHome; }

  public String getHdfsUser() { return hdfsUser; }

  public String getCertificatesDir() { return certificatesDir; }

  public String getImageName() { return imageName; }

  public String getCommandLogFile() { return commandLogFile; }

  public String getHopsfsMountLogFile() { return hopsfsMountLogFile; }

  public String getProjectName() { return projectName; }

  public String getGitCommand() { return gitCommand; }

  public String getCommandConfiguration() { return commandConfiguration; }

  public String getTokenPath() { return tokenPath; }

  public String getExecutionId() { return executionId; }

  public String getProjectId() { return projectId; }

  public String getGitUsername() { return gitUsername; }

  public String getGitToken() { return gitToken; }

  public String getRepositoryId() { return repositoryId; }
}
