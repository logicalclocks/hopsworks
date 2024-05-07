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
package io.hops.hopsworks.common.dao.git;

public class GitPaths {
  private String secret;
  private String gitPath;
  private String confDirPath;
  private String logDirPath;
  private String runDirPath;
  private String tokenPath;
  private String certificatesDirPath;

  public GitPaths(String privateDir, String secretConfig) {
    this.gitPath = privateDir + secretConfig;
    String gitPathWithSlash = this.gitPath.endsWith("/") ? this.gitPath  : this.gitPath + "/";
    this.logDirPath = gitPathWithSlash + "git_logs";
    this.confDirPath = gitPathWithSlash + "conf";
    this.certificatesDirPath = gitPathWithSlash + "certificates";
    this.runDirPath = gitPathWithSlash + "run";
    this.tokenPath = gitPathWithSlash + "token";
    this.secret = secretConfig;
  }

  public String getGitPath() { return gitPath; }

  public void setGitPath(String gitPath) { this.gitPath = gitPath; }

  public String getLogDirPath() { return logDirPath; }

  public void setLogDirPath(String logDirPath) { this.logDirPath = logDirPath; }

  public String getCertificatesDirPath() { return certificatesDirPath; }

  public void setCertificatesDirPath(String certificatesDirPath) { this.certificatesDirPath = certificatesDirPath; }

  public String getRunDirPath() { return runDirPath; }

  public void setRunDirPath(String runDirPath) { this.runDirPath = runDirPath; }

  public String getSecret() { return secret; }

  public void setSecret(String secret) { this.secret = secret; }

  public String getTokenPath() { return tokenPath; }

  public void setTokenPath(String tokenPath) { this.tokenPath = tokenPath; }

  public String getConfDirPath() { return confDirPath; }

  public void setConfDirPath(String confDirPath) { this.confDirPath = confDirPath; }
}
