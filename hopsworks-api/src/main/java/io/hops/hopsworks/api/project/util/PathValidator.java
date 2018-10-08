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
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  public DsPath validatePath(Project project, String path) throws DatasetException, ProjectException {
    DsPath dsPath = new DsPath();

    Pattern authorityPattern = Pattern.compile("(hdfs://[a-zA-Z0-9\\-\\.]{2,255}:[0-9]{4,6})(/.*$)");
    Matcher urlMatcher = authorityPattern.matcher(path);

    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path was not provided.");
    } else if (urlMatcher.find()) {
      // Case hdfs://10.0.2.15:8020//Projects/project1/ds/dsRelativePath
      path = urlMatcher.group(2);
      dsPath.setFullPath(new Path(path));
      String[] pathComponents = path.split("/");
      buildProjectDsRelativePath(pathComponents, dsPath);
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
                             DsPath dsPath) throws DatasetException {
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
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
      }
      dsName = shardDS[1];
      shared = true;
    }

    Dataset ds = datasetFacade.findByNameAndProjectId(project, dsName);
    if (ds == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
    }

    // If the dataset is shared, make sure that the user can access it
    if (shared && (ds.getStatus() == Dataset.PENDING)) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_PENDING, Level.FINE, "datasetId: " + ds.getId());
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
                                          DsPath dsPath) throws ProjectException, DatasetException {
    // Start by 1 as the first component is ""
    Project project = projectFacade.findByName(pathComponents[2]);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE);
    }
    Dataset ds = datasetFacade.findByNameAndProjectId(project, pathComponents[3]);
    if (ds == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
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
                                       DsPath dsPath) throws DatasetException {
    String dsPathStr = File.separator + buildRelativePath(pathComponents, 1, 5);
    Inode dsInode = inodeFacade.getInodeAtPath(dsPathStr);

    if (dsInode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_NOT_FOUND, Level.FINE);
    }

    List<Dataset> dss = datasetFacade.findByInode(dsInode);
    if (dss == null || dss.isEmpty()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
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

