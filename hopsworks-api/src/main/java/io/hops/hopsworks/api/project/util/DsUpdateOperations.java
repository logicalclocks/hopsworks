/*
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
 */

package io.hops.hopsworks.api.project.util;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

/**
 * Helper bean for dataset deletion and creation methods that are shared across several services
 */
@Stateless
public class DsUpdateOperations {

  @EJB
  private PathValidator pathValidator;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DatasetController datasetController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private InodeController inodeController;

  /**
   * Creates a directory inside a top-level dataset
   *
   * @param project       the project of the user making the request
   * @param user          the user making the request
   * @param directoryName the name of the file to create
   * @param description   the description of the file to create
   * @param templateId    the templateId of the file to create
   * @param isSearchable  boolean flag whether the file should be searchable or not
   * @return the fullpath to the created file
   * @throws DatasetException
   * @throws ProjectException
   * @throws HopsSecurityException
   */
  public org.apache.hadoop.fs.Path createDirectoryInDataset(
      Project project, Users user, String directoryName, String description, int templateId, Boolean isSearchable)
      throws DatasetException, ProjectException, HopsSecurityException, UnsupportedEncodingException {
    DsPath dsPath = pathValidator.validatePath(project, directoryName);
    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();

    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      dfso = dfs.getDfsOps();
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      if (username != null) {
        udfso = dfs.getDfsOps(username);
      }
      datasetController.createSubDirectory(project, fullPath, templateId, description, isSearchable, udfso);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    return fullPath;
  }

  /**
   * Deletes a file inside a top-level dataset
   *
   * @param project  the project of the user making the request
   * @param user     the user making the request
   * @param fileName the name of the folder or file to remove
   * @return the fullpath of the deleted file
   * @throws DatasetException
   * @throws ProjectException
   */
  public org.apache.hadoop.fs.Path deleteDatasetFile(
      Project project, Users user, String fileName) throws DatasetException, ProjectException, HopsSecurityException,
      UnsupportedEncodingException {
    boolean success = false;
    DistributedFileSystemOps dfso = null;
    DsPath dsPath = pathValidator.validatePath(project, fileName);
    Dataset ds = dsPath.getDs();

    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    org.apache.hadoop.fs.Path dsRelativePath = dsPath.getDsRelativePath();

    if (dsRelativePath.depth() == 0) {
      throw new IllegalArgumentException("Use endpoint DELETE /{datasetName} to delete top level dataset)");
    }

    try {
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = datasetController.getOwningProject(ds);
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && projectTeamFacade.findCurrentRole(owning, user)
          .equals(AllowedProjectRoles.DATA_OWNER) && owning.equals(project)) {
        dfso = dfs.getDfsOps();// do it as super user
      } else {
        dfso = dfs.getDfsOps(username);// do it as project user
      }
      success = dfso.rm(fullPath, true);
    } catch(AccessControlException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.FINE,
          "Operation: delete, path: " + fullPath.toString(), ex.getMessage(), ex);
    }
    catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.SEVERE,
          "path: " + fullPath.toString(), ex.getMessage(), ex);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
    if (!success) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.FINE,
          "path: " + fullPath.toString());
    }
    return fullPath;
  }

  /**
   * Moves/Renames a file within a Top-level dataset
   *
   * @param project the project of the user making the request
   * @param user the user making the request
   * @param sourceInode the inode to move/rename
   * @param destPathStr the destination path
   * @return the fullpath of the moved file
   * @throws DatasetException
   * @throws ProjectException
   */
  public org.apache.hadoop.fs.Path moveDatasetFile(Project project, Users user, Inode sourceInode, String destPathStr)
      throws DatasetException, ProjectException, HopsSecurityException, UnsupportedEncodingException {
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    String sourcePathStr = inodeController.getPath(sourceInode);
    DsPath sourceDsPath = pathValidator.validatePath(project, sourcePathStr);
    DsPath destDsPath = pathValidator.validatePath(project, destPathStr);

    Dataset sourceDataset = sourceDsPath.getDs();

    // The destination dataset project is already the correct one, as the path is given
    // (and parsed)
    Dataset destDataset = destDsPath.getDs();

    if (!datasetController.getOwningProject(sourceDataset).equals(
        destDataset.getProject())) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_FORBIDDEN, Level.FINE,
          "Cannot copy file/folder from another project.");
    }

    if (destDataset.isPublicDs()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_FORBIDDEN, Level.FINE,
          "Can not move to a public dataset.");
    }

    org.apache.hadoop.fs.Path sourcePath = sourceDsPath.getFullPath();
    org.apache.hadoop.fs.Path destPath = destDsPath.getFullPath();

    DistributedFileSystemOps udfso = null;
    //We need super-user to change owner
    DistributedFileSystemOps dfso = null;
    try {
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = datasetController.getOwningProject(sourceDataset);
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && projectTeamFacade.findCurrentRole(owning, user)
          .equals(AllowedProjectRoles.DATA_OWNER) && owning.equals(project)) {
        udfso = dfs.getDfsOps();// do it as super user
      } else {
        udfso = dfs.getDfsOps(username);// do it as project user
      }
      dfso = dfs.getDfsOps();
      if (udfso.exists(destPath.toString())) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
            "destination: " + destPath.toString());
      }

      //Get destination folder permissions
      FsPermission permission = udfso.getFileStatus(destPath.getParent()).getPermission();
      String group = udfso.getFileStatus(destPath.getParent()).getGroup();
      String owner = udfso.getFileStatus(sourcePath).getOwner();

      udfso.moveWithinHdfs(sourcePath, destPath);

      // Change permissions recursively
      datasetController.recChangeOwnershipAndPermission(destPath, permission,
          owner, group, dfso, udfso);
    } catch(AccessControlException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.FINE,
          "Operation: move, from: " + sourcePathStr + " to: " + destPathStr);
    }
    catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.SEVERE,
          "move operation failed for: " + sourcePathStr, ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
      if (dfso != null) {
        dfso.close();
      }
    }
    return destPath;
  }
}
