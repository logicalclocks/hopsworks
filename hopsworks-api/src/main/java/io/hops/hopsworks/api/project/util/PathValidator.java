package io.hops.hopsworks.api.project.util;

import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;

@Stateless
public class PathValidator {

  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DatasetController datasetContoller;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private Settings settings;

  /**
   * Validate a path received by a DatasetService.java REST API
   * the function validates that the path requests is valid and, in case the dataset is shared,
   * that is not pending.
   *
   * @return A DsPath object containing a reference to the dataset, the full path
   * of the file/dir involved in the operation and the dsRelativePath (the path of the
   * file/directory) starting from the dataset path
   * @throws AppException - in case of bad request or the dataset cannot be found
   */

  public DsPath validatePath(Project project, String path) throws AppException {
    DsPath dsPath = new DsPath();

    if (path == null || path.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.EMPTY_PATH);
    } else if (path.startsWith(File.separator + Settings.DIR_ROOT)) {
      // Case /Projects/project1/ds/dsRelativePath
      dsPath.setFullPath(new Path(path));
      String[] pathComponents = path.split("/");
      buildProjectDsRelativePath(pathComponents, dsPath);
    } else if (path.startsWith(this.settings.getHiveWarehouse())) {
      // Case /apps/hive/warehouse/project1.db/dsRelativePath
      dsPath.setFullPath(new Path(path));
      String[] pathComponents = path.split("/");
      buildHiveDsRelativePath(project, pathComponents, dsPath);
    } else {
      // Case ds/dsRelativePath
      buildFullPath(project, path, dsPath);
    }

    return dsPath;
  }


  private void buildFullPath(Project project, String path,
                             DsPath dsPath) throws AppException {
    //Strip leading slashes.
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    String[] pathComponents = path.split(File.separator);

    String dsName = pathComponents[0];
    boolean shared = false;

    if (pathComponents[0].contains(Settings.SHARED_FILE_SEPARATOR)) {
      //we can split the string and get the project name
      String[] shardDS = pathComponents[0].split(Settings.SHARED_FILE_SEPARATOR);
      if (shardDS.length != 2) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            ResponseMessages.DATASET_NOT_FOUND);
      }
      dsName = shardDS[1];
      shared = true;
    }

    Dataset ds = datasetFacade.findByNameAndProjectId(project, dsName);
    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_FOUND);
    }

    // If the dataset is shared, make sure that the user can access it
    if (shared && (ds.getStatus() == Dataset.PENDING)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_PENDING);
    }
    dsPath.setDs(ds);

    String dsRelativePathStr = buildRelativePath(pathComponents, 1,
        pathComponents.length);

    if (dsRelativePathStr.isEmpty()) {
      dsPath.setFullPath(datasetContoller.getDatasetPath(ds));
    } else {
      Path dsRelativePath = new Path(dsRelativePathStr);
      dsPath.setDsRelativePath(dsRelativePath);
      dsPath.setFullPath(new Path(datasetContoller.getDatasetPath(ds), dsRelativePath));
    }
  }

  private void buildProjectDsRelativePath(String[] pathComponents,
                                          DsPath dsPath) throws AppException {
    // Start by 1 as the first component is ""
    Project project = projectFacade.findByName(pathComponents[2]);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }

    Dataset ds = datasetFacade.findByNameAndProjectId(project, pathComponents[3]);
    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_FOUND);
    }
    dsPath.setDs(ds);

    String dsRelativePathStr = buildRelativePath(pathComponents, 4,
        pathComponents.length);
    if (!dsRelativePathStr.isEmpty()) {
      dsPath.setDsRelativePath(new Path(dsRelativePathStr));
    }
  }

  private void buildHiveDsRelativePath(Project project,
                                       String[] pathComponents,
                                       DsPath dsPath) throws AppException {
    String dsPathStr = File.separator + buildRelativePath(pathComponents, 1, 5);
    Inode dsInode = inodeFacade.getInodeAtPath(dsPathStr);

    if (dsInode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PATH_NOT_FOUND);
    }

    List<Dataset> dss = datasetFacade.findByInode(dsInode);
    if (dss == null || dss.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_FOUND);
    }

    Project owningProject = datasetContoller.getOwningProject(dss.get(0));
    Dataset originalDataset = datasetFacade.findByProjectAndInode(owningProject,
        dss.get(0).getInode());

    dsPath.setDs(originalDataset);

    String dsRelativePathStr = buildRelativePath(pathComponents, 5,
        pathComponents.length);
    if (!dsRelativePathStr.isEmpty()) {
      dsPath.setDsRelativePath(new Path(dsRelativePathStr));
    }
  }

  private String buildRelativePath(String[] pathComponents, int start, int stop) {
    StringBuilder pathBuilder = new StringBuilder();
    int i;
    for (i = start; i < stop -1 ; i++) {
      pathBuilder.append(pathComponents[i]).append(File.separator);
    }
    // avoid putting the / at the end of the path
    if (i == stop -1) {
      pathBuilder.append(pathComponents[i]);
    }

    return pathBuilder.toString();
  }
}

