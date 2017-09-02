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
