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
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWith;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class PathValidator {

  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetSharedWithFacade datasetSharedWithFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private Settings settings;

  private static final Logger LOGGER = Logger.getLogger(PathValidator.class.getName());

  /**
   * Validate a path received by a DatasetService.java REST API
   * the function validates that the path requests is valid and, in case the dataset is shared,
   * that is not pending.
   *
   * @return A DsPath object containing a reference to the dataset, the full path
   * of the file/dir involved in the operation and the dsRelativePath (the path of the
   * file/directory) starting from the dataset path
   */

  public DsPath validatePath(Project project, String path) throws DatasetException, ProjectException,
      UnsupportedEncodingException {
    DsPath dsPath = new DsPath();
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path was not provided.");
    } 
    
    path = Utils.prepPath(path);
    
    if (path.startsWith(File.separator + Settings.DIR_ROOT)) {
      // Case /Projects/project1/ds/dsRelativePath
      dsPath.setFullPath(new Path(path));
      String[] pathComponents = path.split("/");
      buildProjectDsRelativePath(project, pathComponents, dsPath);
    } else if (path.startsWith(this.settings.getHiveWarehouse())) {
      // Case /apps/hive/warehouse/project1.db/
      dsPath.setFullPath(new Path(path));
      String[] pathComponents = path.split("/");
      buildHiveDsRelativePath(project, pathComponents, dsPath);
    } else {
      // Case ds/dsRelativePath
      buildFullPath(project, path, dsPath);
    }

    return dsPath;
  }


  private void buildFullPath(Project project, String path, DsPath dsPath) throws DatasetException {
    //Strip leading slashes.
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    String[] pathComponents = path.split(File.separator);

    String dsName = pathComponents[0];
    boolean shared = false;
    String parentProjectPath = null;

    if (pathComponents[0].contains(Settings.SHARED_FILE_SEPARATOR)) {
      //we can split the string and get the project name
      String[] shardDS = pathComponents[0].split(Settings.SHARED_FILE_SEPARATOR);
      if (shardDS.length != 2) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
      }
      parentProjectPath = Utils.getProjectPath(shardDS[0]);
      dsName = shardDS[1];
      shared = true;
    }
    Dataset ds = datasetController.getByProjectAndDsName(project, parentProjectPath, dsName);
    if (ds == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
    }

    // If the dataset is shared, make sure that the user can access it
    if (shared) {
      DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(project, ds);
      if (datasetSharedWith != null && !datasetSharedWith.getAccepted()) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_PENDING, Level.FINE, "datasetId: " + ds.getId());
      }
    }
    dsPath.setDs(ds);

    String dsRelativePathStr = buildRelativePath(pathComponents, 1, pathComponents.length);

    if (dsRelativePathStr.isEmpty()) {
      dsPath.setFullPath(datasetController.getDatasetPath(ds));
    } else {
      Path dsRelativePath = new Path(dsRelativePathStr);
      dsPath.setDsRelativePath(dsRelativePath);
      Path fullPath = new Path(datasetController.getDatasetPath(ds), dsRelativePath);
      dsPath.setFullPath(fullPath);
    }
  }
  
  private void buildProjectDsRelativePath(Project project, String[] pathComponents, DsPath dsPath)
    throws ProjectException, DatasetException {
    // Start by 1 as the first component is ""
    Project destProject = projectFacade.findByName(pathComponents[2]);
    if (project == null || destProject == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE);
    }
    Dataset ds = datasetController.getByProjectAndDsName(project, Utils.getProjectPath(pathComponents[2]),
      pathComponents[3]);
    if (ds == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
    }
    dsPath.setDs(ds);
    
    String dsRelativePathStr = buildRelativePath(pathComponents, 4, pathComponents.length);
    if (!dsRelativePathStr.isEmpty()) {
      dsPath.setDsRelativePath(new Path(dsRelativePathStr));
    }
  }

  private void buildHiveDsRelativePath(Project project, String[] pathComponents, DsPath dsPath)
    throws DatasetException {
    String dsPathStr = File.separator + buildRelativePath(pathComponents, 1, 5);
    Inode dsInode = inodeController.getInodeAtPath(dsPathStr);

    if (dsInode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_NOT_FOUND, Level.FINE);
    }

    Dataset originalDataset = datasetFacade.findByInode(dsInode);
    if (originalDataset == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
    }

    dsPath.setDs(originalDataset);

    String dsRelativePathStr = buildRelativePath(pathComponents, 5, pathComponents.length);
    if (!dsRelativePathStr.isEmpty()) {
      dsPath.setDsRelativePath(new Path(dsRelativePathStr));
    }
  }

  private String buildRelativePath(String[] pathComponents, int start, int stop) {
    StringBuilder pathBuilder = new StringBuilder();
    int i;
    for (i = start; i < stop -1 ; i++) {
      if (!pathComponents[i].isEmpty() && !pathComponents[i].equals("..")) {
        pathBuilder.append(pathComponents[i]).append(File.separator);
      }
    }
    // avoid putting the / at the end of the path
    if (i == stop -1) {
      pathBuilder.append(pathComponents[i]);
    }

    return pathBuilder.toString();
  }
}

