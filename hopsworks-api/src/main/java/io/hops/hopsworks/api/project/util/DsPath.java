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

package io.hops.hopsworks.api.project.util;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import java.util.logging.Level;

/**
 * This class is returned from the PathValidator which parses the PATHs
 * received from the DatasetService.java
 * It contains information related to the dataset involved in the REST call,
 * the full path of the file or directory involved
 * and the dsRelativePath, which is the path of the file/directory,
 * relative to the dataset path
 */
public class DsPath {
  private Dataset ds = null;
  private Path fullPath = null;
  private Path dsRelativePath = null;

  public Dataset getDs() {
    return ds;
  }

  public void setDs(Dataset ds) {
    this.ds = ds;
  }

  public Path getFullPath() {
    return fullPath;
  }

  public void setFullPath(Path fullPath) {
    this.fullPath = fullPath;
  }

  public Path getDsRelativePath() {
    return dsRelativePath;
  }

  public void setDsRelativePath(Path dsRelativePath) {
    this.dsRelativePath = dsRelativePath;
  }

  public Inode validatePathExists(InodeController inodeController,
                                  Boolean dir) throws DatasetException {
    Inode inode = inodeController.getInodeAtPath(fullPath.toString());
    if (inode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_FOUND, Level.FINE, "path: " + fullPath.toString());
    }

    if (dir != null && dir && !inode.isDir()){
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_DIRECTORY,  Level.FINE,
        "path: " + fullPath.toString());
    }

    if (dir != null && !dir && inode.isDir()){
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_IS_DIRECTORY, Level.FINE,
        "path: " + fullPath.toString());
    }

    return inode;
  }
}
