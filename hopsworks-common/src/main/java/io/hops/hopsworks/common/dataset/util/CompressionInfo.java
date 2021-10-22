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
package io.hops.hopsworks.common.dataset.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.fs.Path;

public class CompressionInfo {

  private Path hdfsPath;
  private Path outputPath;
  private String stagingDirectory;

  public CompressionInfo(Path hdfsPath, Path outputPath) {
    this.hdfsPath = hdfsPath;
    this.outputPath = outputPath;
    if(outputPath != null) {
      this.setStagingDirectory(DigestUtils.sha256Hex(hdfsPath.toString() + "_" + outputPath.toString()));
    } else {
      this.setStagingDirectory(DigestUtils.sha256Hex(hdfsPath.toString()));
    }
  }

  public Path getHdfsPath() {
    return hdfsPath;
  }

  public void setHdfsPath(Path hdfsPath) {
    this.hdfsPath = hdfsPath;
  }

  public Path getOutputPath() {
    return outputPath;
  }

  public void setOutputPath(Path outputPath) {
    this.outputPath = outputPath;
  }

  public String getStagingDirectory() {
    return stagingDirectory;
  }

  public void setStagingDirectory(String stagingDirectory) {
    this.stagingDirectory = stagingDirectory;
  }

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof CompressionInfo)) {
      return false;
    }
    CompressionInfo cinfo = (CompressionInfo) obj;
    return hdfsPath.equals(cinfo.hdfsPath);
  }
}
