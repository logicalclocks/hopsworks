/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.upload;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.Path;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basically provides a temporary folder in which to stage uploaded files.
 */
@Singleton
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class StagingManager {
  private static final Logger LOGGER = Logger.getLogger(StagingManager.class.getName());
  private Path hdfsStagingFolder;

  @EJB
  private Settings settings;
  @EJB
  private DistributedFsService dfs;

  @PostConstruct
  public void init() {
    hdfsStagingFolder = new Path(settings.getUploadStagingDir());
    createStagingDir(hdfsStagingFolder);
  }
  
  private void createStagingDir(Path stagingDir) {
    DistributedFileSystemOps dfsOps = null;
    try {
      dfsOps = dfs.getDfsOps();
      if (!dfsOps.exists(stagingDir) && !isRootDir(stagingDir.toString())) {
        boolean created = dfsOps.mkdirs(stagingDir, dfsOps.getParentPermission(stagingDir));
        if (!created) {
          LOGGER.log(Level.WARNING, "Failed to create upload staging dir. Path: {0}", stagingDir.toString());
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to create upload staging dir. Path: {0}", stagingDir.toString());
    } finally {
      if (dfsOps != null) {
        dfs.closeDfsClient(dfsOps);
      }
    }
  }
  
  public String getStagingPath() {
    // if hdfs staging dir is set to Projects return empty b/c upload path will contain /Projects
    if (isRootDir(hdfsStagingFolder.toString())) {
      return "";
    }
    //If staging dir not created or changes recreate
    if (hdfsStagingFolder == null || !hdfsStagingFolder.equals(new Path(settings.getUploadStagingDir()))) {
      hdfsStagingFolder = new Path(settings.getUploadStagingDir());
      createStagingDir(hdfsStagingFolder);
    }
    return hdfsStagingFolder.toString();
  }
  
  private boolean isRootDir(String path) {
    return path.equals(Settings.DIR_ROOT) || path.equals("/" + Settings.DIR_ROOT);
  }
}
