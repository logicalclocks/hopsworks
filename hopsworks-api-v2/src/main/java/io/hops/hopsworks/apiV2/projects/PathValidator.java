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

