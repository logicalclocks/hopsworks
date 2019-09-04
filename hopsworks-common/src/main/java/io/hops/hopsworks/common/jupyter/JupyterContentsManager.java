/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.jupyter;

public enum JupyterContentsManager {
  
  HDFS_CONTENTS_MANAGER("hdfscontents.hdfsmanager.HDFSContentsManager"),
  LARGE_FILE_CONTENT_MANAGER("notebook.services.contents.largefilemanager.LargeFileManager");
  
  private final String className;
  
  JupyterContentsManager(String className) {
    this.className = className;
  }
  
  public String getClassName() {
    return className;
  }
}
