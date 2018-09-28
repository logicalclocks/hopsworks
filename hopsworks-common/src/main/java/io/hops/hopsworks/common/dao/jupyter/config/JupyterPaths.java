/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.jupyter.config;

import io.hops.hopsworks.common.util.Settings;

import java.io.File;

public class JupyterPaths {

  private String projectUserPath;
  private String notebookPath;
  private String confDirPath;
  private String logDirPath;
  private String runDirPath;
  private String certificatesDir;

  public JupyterPaths(String jupyterDir, String projectName, String hdfsUser, String secretConfig) {
    projectUserPath = jupyterDir + File.separator + Settings.DIR_ROOT +
        File.separator + projectName + File.separator + hdfsUser;
    notebookPath = projectUserPath + File.separator + secretConfig;
    confDirPath = notebookPath + File.separator + "conf";
    logDirPath = notebookPath + File.separator + "logs";
    runDirPath = notebookPath + File.separator + "run";
    certificatesDir = notebookPath + File.separator + "certificates";
  }

  public String getProjectUserPath() {
    return projectUserPath;
  }

  public String getNotebookPath() {
    return notebookPath;
  }

  public String getConfDirPath() {
    return confDirPath;
  }

  public String getLogDirPath() {
    return logDirPath;
  }

  public String getRunDirPath() {
    return runDirPath;
  }

  public String getCertificatesDir() {
    return certificatesDir;
  }
}
