/*
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
 *
 */

package io.hops.hopsworks.api.project.util;

import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.exception.AppException;
import org.apache.hadoop.fs.Path;

import javax.ws.rs.core.Response;

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

  public Inode validatePathExists(InodeFacade ifacade,
                                  Boolean dir) throws AppException {
    Inode inode = ifacade.getInodeAtPath(fullPath.toString());
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PATH_NOT_FOUND);
    }

    if (dir != null && dir && !inode.isDir()){
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PATH_NOT_DIRECTORY);
    }

    if (dir != null && !dir && inode.isDir()){
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PATH_IS_DIRECTORY);
    }

    return inode;
  }
}
