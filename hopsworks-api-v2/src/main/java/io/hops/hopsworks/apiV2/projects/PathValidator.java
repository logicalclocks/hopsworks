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

package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

@Stateless
public class PathValidator {
  
  @EJB
  private DatasetController datasetContoller;
  
  public Path getFullPath(DatasetPath path){
    String relativePath = path.getRelativePath();
    Path datasetPath = datasetContoller.getDatasetPath(path.getDataSet());
    Path toReturn;
    if (relativePath.isEmpty() || "/".equals(relativePath)){
      toReturn = datasetPath;
    } else {
      //Strip leading slashes
      while (relativePath.startsWith("/")) {
        relativePath = relativePath.substring(1);
      }
      toReturn = new Path(datasetPath, relativePath);
    }
    
    return toReturn;
  }
  
  public Inode exists(DatasetPath path, InodeFacade ifacade,
               Boolean dir) throws AppException {
    Path fullPath = getFullPath(path);
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

