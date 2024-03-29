/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.dataset.util;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatasetHelper {
  
  private static final Logger LOGGER = Logger.getLogger(DatasetHelper.class.getName());
  private static final int MAX_INODES_TOBE_RETURNED = 10000;
  
  @EJB
  private DatasetSharedWithFacade datasetSharedWithFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private InodeController inodeController;
  @EJB
  private Settings settings;

  public DatasetPath getNewDatasetPath(Project project, String path, DatasetType datasetType) throws DatasetException {
    String rootDir;
    if (datasetType == null) {
      rootDir = path.startsWith(settings.getHiveWarehouse()) ? settings.getHiveWarehouse() : Settings.DIR_ROOT;
    } else {
      rootDir = datasetType.equals(DatasetType.DATASET) ? Settings.DIR_ROOT : settings.getHiveWarehouse();
    }
    DatasetPath datasetPath;
    try {
      datasetPath = new DatasetPath(project, path, rootDir);
    } catch (UnsupportedEncodingException e) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_ENCODING_NOT_SUPPORTED, Level.FINE);
    }
    return datasetPath;
  }
  
  public DatasetPath updateDataset(Project project, DatasetPath datasetPath) throws DatasetException {
    Dataset dataset = datasetController.getByProjectAndFullPath(project, datasetPath.getDatasetFullPath().toString());
    return updateDataset(project, datasetPath, dataset);
  }
  
  public DatasetPath updateDataset(Project project, DatasetPath datasetPath, Dataset dataset) {
    datasetPath.setDataset(dataset);
    if (dataset != null && dataset.isShared(project)) {
      DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(project, dataset);
      datasetPath.setDatasetSharedWith(datasetSharedWith);
    }
    return datasetPath;
  }
  
  /**
   *
   * @param project
   * @param path the path should be a absolute path within projects and hive warehouse
   * @return
   * @throws DatasetException
   */
  public DatasetPath getDatasetPathFromAbsolute(Project project, String path) throws DatasetException {
    return getDatasetPath(project, path, null);
  }

  public DatasetPath getDatasetPath(Project project, String path, DatasetType datasetType) throws DatasetException {
    DatasetPath datasetPath = getNewDatasetPath(project, path, datasetType);
    Dataset dataset = datasetController.getByProjectAndFullPath(project, datasetPath.getDatasetFullPath().toString());
    if (dataset == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "path: " + path);
    }
    datasetPath.setDataset(dataset);
    if (dataset.isShared(project)) {
      DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(project, dataset);
      datasetPath.setDatasetSharedWith(datasetSharedWith);
    }
    //This line would benefit from an Inode cache on /Projects or even /Projects/project level
    Inode projectInode = inodeController.getProjectRoot(dataset.getProject().getName());
    Inode datasetInode = inodeController.getProjectDatasetInode(projectInode,
      datasetPath.getDatasetFullPath().toString(), datasetPath.getDataset());
    if (datasetPath.isTopLevelDataset()) {
      datasetPath.setInode(datasetInode);
      return datasetPath;
    } else {
      datasetPath.setInode(inodeController.getInodeAtPath(datasetInode,
        datasetPath.getDatasetFullPath().depth(), datasetPath.getDatasetRelativePath()));// expensive
    }
    return datasetPath;
  }
  
  public DatasetPath getTopLevelDatasetPath(Project project, Dataset dataset, Inode dsInode) throws DatasetException {
    DatasetPath datasetPath;
    if(dataset.isShared(project)) {
      DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(project, dataset);
      datasetPath = getTopLevelDatasetPath(project, datasetSharedWith, dsInode);
    } else {
      datasetPath = getNewDatasetPath(project, dataset.getName(), dataset.getDsType());
      datasetPath.setDataset(dataset);
      datasetPath.setInode(dsInode);
    }
    return datasetPath;
  }
  
  public DatasetPath getTopLevelDatasetPath(Project project, DatasetSharedWith datasetSharedWith,
                                            Inode datasetSharedWithInode)
    throws DatasetException {
    String path = datasetSharedWith.getDatasetName();
    DatasetPath datasetPath = getNewDatasetPath(project, path, datasetSharedWith.getDataset().getDsType());
    datasetPath.setDataset(datasetSharedWith.getDataset());
    datasetPath.setDatasetSharedWith(datasetSharedWith);
    datasetPath.setInode(datasetSharedWithInode);
    return datasetPath;
  }
  
  public DatasetPath getDatasetPathIfFileExist(Project project, String path, DatasetType datasetType)
    throws DatasetException {
    DatasetPath datasetPath = getDatasetPath(project, path, datasetType);
    checkFileExist(datasetPath);
    return datasetPath;
  }
  
  public void checkFileExist (DatasetPath datasetPath) throws DatasetException {
    if (datasetPath.getInode() == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_FOUND, Level.FINE);
    }
  }
  
  public void checkResourceRequestLimit(ResourceRequest resourceRequest, int childrenNum) {
    int limit = MAX_INODES_TOBE_RETURNED;
    if (childrenNum > limit && (resourceRequest.getLimit() == 0 || resourceRequest.getLimit() > limit)) {
      //maybe notify user
      resourceRequest.setLimit(limit);
    }
  }

  public void checkIfDatasetExists(Project project, DatasetPath datasetPath) throws DatasetException {
    datasetController.getByProjectAndFullPath(project, datasetPath.getDatasetFullPath().toString());
  }

  public boolean isBasicDatasetProjectParent(Project project, DatasetPath datasetPath) {
    String basicDataset = Path.SEPARATOR + Settings.DIR_ROOT + Path.SEPARATOR + project.getName();
    String datasetFullPath = datasetPath.getFullPath().toString();
    return datasetFullPath.startsWith(basicDataset);
  }
}
