/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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

