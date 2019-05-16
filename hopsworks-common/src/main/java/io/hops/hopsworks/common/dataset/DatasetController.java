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

package io.hops.hopsworks.common.dataset;

import io.hops.common.Pair;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetPermissions;
import io.hops.hopsworks.common.dao.dataset.DatasetRequest;
import io.hops.hopsworks.common.dao.dataset.DatasetRequestFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWith;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.log.operation.OperationsLog;
import io.hops.hopsworks.common.dao.log.operation.OperationsLogFacade;
import io.hops.hopsworks.common.dao.metadata.InodeBasicMetadata;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.dao.metadata.db.InodeBasicMetadataFacade;
import io.hops.hopsworks.common.dao.metadata.db.TemplateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.featorestore.FeaturestoreConstants;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.FsPermissions;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains business logic pertaining DataSet management.
 * <p>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DatasetController {

  private static final Logger LOGGER = Logger.getLogger(DatasetController.class.getName());
  @EJB
  private InodeFacade inodes;
  @EJB
  private InodeController inodeController;
  @EJB
  private TemplateFacade templates;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetSharedWithFacade datasetSharedWithFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private InodeBasicMetadataFacade inodeBasicMetaFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private OperationsLogFacade operationsLogFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private DatasetRequestFacade datasetRequest;
  
  /**
   * Create a new DataSet. This is, a folder right under the project home
   * folder.
   * **The Dataset directory is created using the superuser dfso**
   *
   * @param user The creating Users. Cannot be null.
   * @param project The project under which to create the DataSet. Cannot be
   * null.
   * @param dataSetName The name of the DataSet being created. Cannot be null
   * and must satisfy the validity criteria for a folder name.
   * @param datasetDescription The description of the DataSet being created. Can
   * be null.
   * @param templateId The id of the metadata template to be associated with
   * this DataSet.
   * @param searchable Defines whether the dataset can be indexed or not (i.e.
   * whether it can be visible in the search results or not)
   * @param stickyBit Whether or not the dataset should have the sticky bit set
   * @param defaultDataset
   * @param dfso
   * folder names, or the folder already exists.
   */
  public void createDataset(Users user, Project project, String dataSetName, String datasetDescription, int templateId,
    boolean searchable, boolean stickyBit, boolean defaultDataset, DistributedFileSystemOps dfso)
    throws DatasetException, HopsSecurityException {
    //Parameter checking.
    if (user == null || project == null || dataSetName == null) {
      throw new IllegalArgumentException("User, project or dataset were not provided");
    }
    FolderNameValidator.isValidName(dataSetName, false);
    //Logic
    boolean success;
    String dsPath = Utils.getProjectPath(project.getName()) + dataSetName;
    Inode parent = inodeController.getProjectRoot(project.getName());
    Inode ds = inodes.findByInodePK(parent, dataSetName, HopsUtils.dataSetPartitionId(parent, dataSetName));
    if (ds != null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
        "Dataset name: " + dataSetName);
    }
    //Permission 770
    FsAction global = FsAction.NONE;
    FsAction group = (defaultDataset ? FsAction.ALL : FsAction.READ_EXECUTE);
    FsPermission fsPermission = new FsPermission(FsAction.ALL, group, global, stickyBit);
    success = createFolder(dsPath, templateId, fsPermission, dfso);
    if (success) {
      try {
        ds = inodes.findByInodePK(parent, dataSetName, HopsUtils.dataSetPartitionId(parent, dataSetName));
        Dataset newDS = new Dataset(ds, project);
        newDS.setSearchable(searchable);
        if (datasetDescription != null) {
          newDS.setDescription(datasetDescription);
        }
        datasetFacade.persistDataset(newDS);
        activityFacade.persistActivity(ActivityFacade.NEW_DATA + dataSetName, project, user, ActivityFlag.DATASET);
        // creates a dataset and adds user as owner.
        hdfsUsersBean.addDatasetUsersGroups(user, project, newDS, dfso);
        //set the dataset meta enabled. Support 3 level indexing
        if (searchable) {
          dfso.setMetaEnabled(dsPath);
          Dataset logDs = getByProjectAndDsName(project,null, dataSetName);
          logDataset(project, logDs, OperationType.Add);
        }
      } catch (Exception e) {
        try {
          dfso.rm(new Path(dsPath), true); //if dataset persist fails rm ds folder.
        } catch (IOException ex) {
          // Dataset clean up failed. Log the exception for further debugging.
          LOGGER.log(Level.SEVERE, "Could not cleanup dataset dir after exception: " + dsPath, ex);
        }
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.SEVERE,
            "Could not create dataset: " + dataSetName, e.getMessage(), e);
      }
    } else {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.INFO,
        "Could not create dataset: " + dataSetName);
    }
  }

  /**
   * Create a directory under an existing DataSet. With the same permission as
   * the parent.
   * The directory is created using the user dfso
   *
   * @param project The project under which the directory is being created.
   * Cannot be null.
   *
   * @param dirPath The full path of the folder to be created.
   * /Projects/projectA/datasetB/folder1/folder2/folder3, folder1/folder2
   * has to exist and folder3 needs to be a valid name.
   * @param templateId The id of the template to be associated with the newly
   * created directory.
   * @param description The description of the directory
   * @param searchable Defines if the directory can be searched upon
   * @param udfso
   * the directory.
   * @throws IllegalArgumentException If:
   * <ul>
   * <li>Any of the folder names on the given path does not have a valid name or
   * </li>
   * <li>Such a folder already exists. </li>
   * <li>The parent folder does not exists. </li>
   * </ul>
   * @see FolderNameValidator
   * @throws NullPointerException If any of the non-null-allowed parameters is
   * null.
   */
  public void createSubDirectory(Project project, Path dirPath, int templateId, String description, boolean searchable,
    DistributedFileSystemOps udfso) throws DatasetException, HopsSecurityException {
    if (project == null) {
      throw new NullPointerException("Cannot create a directory under a null project.");
    } else if (dirPath == null) {
      throw new NullPointerException("Cannot create a directory for an empty path.");
    }

    String folderName = dirPath.getName();
    String parentPath = dirPath.getParent().toString();
    FolderNameValidator.isValidName(folderName, true);

    //Check if the given folder already exists
    if (inodeController.existsPath(dirPath.toString())) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_SUBDIR_ALREADY_EXISTS, Level.FINE,
          "The given path: " + dirPath.toString() + " already exists");
    }

    // Check if the parent directory exists
    Inode parent = inodeController.getInodeAtPath(parentPath);
    if (parent == null) {
      throw new IllegalArgumentException(
        "Path for parent folder does not exist: " + parentPath + " under " + project.getName());
    }

    //Now actually create the folder
    boolean success = this.createFolder(dirPath.toString(), templateId, null, udfso);

    //if the folder was created successfully, persist basic metadata to it -
    //description and searchable attribute
    if (success) {
      //find the corresponding inode
      long partitionId = HopsUtils.calculatePartitionId(parent.getId(), folderName, dirPath.depth());
      Inode folder = this.inodes.findByInodePK(parent, folderName, partitionId);
      InodeBasicMetadata basicMeta = new InodeBasicMetadata(folder, description, searchable);
      this.inodeBasicMetaFacade.addBasicMetadata(basicMeta);
    }
  }

  /**
   * Deletes a folder recursively as the given user.
   * <p>
   * @param dataset
   * @param location
   * @param udfso
   * @return
   * @throws java.io.IOException
   */
  public boolean deleteDatasetDir(Dataset dataset, Path location,
      DistributedFileSystemOps udfso) throws IOException {
    OperationsLog log = new OperationsLog(dataset, OperationType.Delete);
    udfso.unsetMetaEnabled(location);
    boolean success = udfso.rm(location, true);
    if (success) {
      operationsLogFacade.persist(log);
    }
    return success;
  }
  
  public void recChangeOwnershipAndPermission(Path path, FsPermission permission,
      String username, String group,
      DistributedFileSystemOps dfso,
      DistributedFileSystemOps udfso)
      throws IOException {

    /*
     * TODO: Currently there is no change permission recursively operation
     * available in HOPSFS client. So we build all the path of the tree and
     * we call the set permission on each one
     */
    // Set permission/ownership for the root
    if (username != null && group != null && dfso != null) {
      dfso.setOwner(path, username, group);
    }
    udfso.setPermission(path, permission);
    Inode rootInode = inodeController.getInodeAtPath(path.toString());

    // Keep a list of directories to avoid using recursion
    // Remember also the path to avoid going to the database for path resolution
    Stack<Pair<Inode, Path>> dirs = new Stack<>();
    if (rootInode.isDir()) {
      dirs.push(new Pair<>(rootInode, path));
    }

    while (!dirs.isEmpty()) {
      Pair<Inode, Path> dirInode = dirs.pop();
      for (Inode child : inodeController.getChildren(dirInode.getL())) {
        Path childPath = new Path(dirInode.getR(), child.getInodePK().getName());

        if (username != null && group != null && dfso != null) {
          dfso.setOwner(childPath, username, group);
        }
        udfso.setPermission(childPath, permission);

        if (child.isDir()) {
          dirs.push(new Pair<>(child, childPath));
        }
      }
    }
  }

  /**
   * Creates a folder in HDFS at the given path, and associates a template with
   * that folder.
   * <p/>
   * @param path The full HDFS path to the folder to be created (e.g.
   * /Projects/projectA/datasetB/folder1/folder2).
   * @param template The id of the template to be associated with the created
   * folder.
   * @return
   * @throws IOException
   */
  private boolean createFolder(String path, int template,
      FsPermission fsPermission,
      DistributedFileSystemOps dfso) throws HopsSecurityException {
    boolean success;
    Path location = new Path(path);
    try {
      if (fsPermission == null) {
        fsPermission = dfso.getParentPermission(location);
      }
      success = dfso.mkdir(location, fsPermission);
      if (success) {
        dfso.setPermission(location, fsPermission);
      }
      if (success && template != 0 && template != -1) {
        //Get the newly created Inode.
        Inode created = inodeController.getInodeAtPath(path);
        Template templ = templates.findByTemplateId(template);
        if (templ != null) {
          templ.getInodes().add(created);
          //persist the relationship table
          templates.updateTemplatesInodesMxN(templ);
        }
      }
    } catch (IOException  ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.WARNING, "path: " + path,
        ex.getMessage(), ex);
    }
    return success;
  }

  /**
   * Generates a markdown style README file for a given Dataset.
   *
   * @param udfso
   * @param dsName
   * @param description
   * @param project
   */
  public void generateReadme(DistributedFileSystemOps udfso, String dsName,
      String description, String project) {
    if (udfso != null) {
      String readmeFile, readMeFilePath;
      //Generate README.md for the Default Datasets
      readmeFile = String.format(Settings.README_TEMPLATE, dsName, description);
      StringBuilder readmeSb = new StringBuilder();
      readmeSb.append(Utils.getProjectPath(project)).append(dsName)
          .append(File.separator).append(Settings.README_FILE);

      readMeFilePath = readmeSb.toString();

      try (FSDataOutputStream fsOut = udfso.create(readMeFilePath)) {
        fsOut.writeBytes(readmeFile);
        fsOut.flush();
        udfso.setPermission(new org.apache.hadoop.fs.Path(readMeFilePath), FsPermissions.rwxr_x___);
      } catch (IOException ex) {
        LOGGER.log(Level.WARNING, "README.md could not be generated for project"
            + " {0} and dataset {1}.", new Object[]{project, dsName});
      }
    } else {
      LOGGER.log(Level.WARNING, "README.md could not be generated for project"
          + " {0} and dataset {1}. DFS client was null", new Object[]{
            project,
            dsName});
    }
  }

  /**
   * Get Readme.md file content from path.
   *
   * @param path full path to the readme file
   * @param dfso give user dfso if access control is required.
   * @return
   * @throws AccessControlException if dfso is for a user and this user have no
   * access permission to the file.
   * @throws IOException
   */
  public FilePreviewDTO getReadme(String path, DistributedFileSystemOps dfso)
      throws IOException {
    if (path == null || dfso == null) {
      throw new IllegalArgumentException("One or more arguments are not set.");
    }
    if (!path.endsWith("README.md")) {
      throw new IllegalArgumentException("Path does not contain readme file.");
    }
    FilePreviewDTO filePreviewDTO = null;
    DataInputStream dis = null;
    try {
      if (!dfso.exists(path) || dfso.isDir(path)) {
        throw new IOException("The file does not exist");
      }
      dis = new DataInputStream(dfso.open(path));
      long fileSize = dfso.getFileStatus(new org.apache.hadoop.fs.Path(
          path)).getLen();
      if (fileSize > Settings.FILE_PREVIEW_TXT_SIZE_BYTES) {
        throw new IllegalArgumentException("README.md must be smaller than"
            + Settings.FILE_PREVIEW_TXT_SIZE_BYTES
            + " to be previewd");
      }
      byte[] headContent = new byte[(int) fileSize];
      dis.readFully(headContent, 0, (int) fileSize);
      filePreviewDTO = new FilePreviewDTO("text", "md", new String(headContent));
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You can not view the file.");
    } finally {
      if (dis != null) {
        dis.close();
      }
    }
    return filePreviewDTO;
  }

  public void logDataset(Project project, Dataset dataset, OperationType type) {
    if (dataset.isShared(project) || !dataset.isSearchable()) {
      return;
    }
    operationsLogFacade.persist(new OperationsLog(dataset, type));
  }

  public Path getDatasetPath(Dataset ds) {
    Path path = null;
    switch (ds.getDsType()) {
      case DATASET:
        Project owningProject = getOwningProject(ds);
        path = new Path(Utils.getProjectPath(owningProject.getName()),
            ds.getInode().getInodePK().getName());
        break;
      case FEATURESTORE:
      case HIVEDB:
        path = new Path(settings.getHiveWarehouse(),
            ds.getInode().getInodePK().getName());
    }

    return path;
  }

  public Project getOwningProject(Dataset ds) {
    return ds.getProject();
  }

  public Project getOwningProject(Inode ds) {
    Inode parent = inodes.findParent(ds);
    Project proj = projectFacade.findByName(parent.getInodePK().getName());
    if (proj == null) {
      String datasetName = ds.getInodePK().getName();
      //a hive database
      if (datasetName.endsWith(".db")) {
        String projectName;
        if (datasetName.endsWith(FeaturestoreConstants.FEATURESTORE_HIVE_DB_SUFFIX + ".db")) {
          projectName = datasetName.substring(0, datasetName.lastIndexOf("_"));
        } else {
          projectName = datasetName.substring(0, datasetName.lastIndexOf("."));
        }
        proj = projectFacade.findByNameCaseInsensitive(projectName);
      }
    }
    return proj;
  }
    
  /**
   *
   * @param project
   * @param user
   * @param path
   * @return
   */
  public boolean isDownloadAllowed(Project project, Users user, String path) {
    //Data Scientists are allowed to download their own data
    String role = projectTeamFacade.findCurrentRole(project, user);
    if (role.equals(AllowedRoles.DATA_OWNER)) {
      return true;
    } else if (role.equals(AllowedRoles.DATA_SCIENTIST)) {
      DistributedFileSystemOps udfso = null;
      try {
        String username = hdfsUsersBean.getHdfsUserName(project, user);
        udfso = dfs.getDfsOps(username);
        String owner = udfso.getFileStatus(new org.apache.hadoop.fs.Path(path)).getOwner();
        //Find hdfs user for this project
        String projectUser = hdfsUsersBean.getHdfsUserName(project, user);
        //If user requesting the download is the owner, approve the request
        if (owner.equals(projectUser)) {
          return true;
        }
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "Could not get owner of file: " + path, ex);
      } finally {
        if (udfso != null) {
          dfs.closeDfsClient(udfso);
        }
      }
    }
    return false;
  }
  
  public void unsetMetaEnabledForAllDatasets(DistributedFileSystemOps dfso, Project project) throws IOException {
    Collection<Dataset> datasets = project.getDatasetCollection();
    for (Dataset dataset : datasets) {
      if (dataset.isSearchable() && !dataset.isShared(project)) {
        Path dspath = getDatasetPath(dataset);
        dfso.unsetMetaEnabled(dspath);
      }
    }
  }
  
  public Dataset getDatasetByInodeId(Long inodeId) {
    Inode inode = inodes.findById(inodeId);
    return datasetFacade.findByInode(inode);
  }
  
  /**
   * Get a top level dataset by project name or parent path. If parent path is null the project name is used as parent
   * @param currentProject
   * @param inodeParentPath
   * @param dsName
   * @return
   */
  public Dataset getByProjectAndDsName(Project currentProject, String inodeParentPath, String dsName) {
    Inode parentInode = inodeController.getInodeAtPath(inodeParentPath == null?
      Utils.getProjectPath(currentProject.getName()) : inodeParentPath);
    Inode dsInode = inodes.findByInodePK(parentInode, dsName, HopsUtils.calculatePartitionId(parentInode.getId(),
      dsName, 3));
    if (dsInode == null && dsName.endsWith(".db")) { //if hive parent is not project
      parentInode = inodeController.getInodeAtPath(settings.getHiveWarehouse());
      dsInode = inodes.findByInodePK(parentInode, dsName, HopsUtils.calculatePartitionId(parentInode.getId(),
        dsName, 3));
    }
    if (currentProject == null || dsInode == null) {
      return null;
    }
    return getByProjectAndInode(currentProject, dsInode);
  }
  
  public Dataset getByProjectAndInode(Project project, Inode inode) {
    Dataset dataset = datasetFacade.findByInode(inode);
    if (dataset != null && !dataset.getProject().equals(project)) { //not owned by project check shared
      DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(project, dataset);
      if (datasetSharedWith == null) {
        dataset = null;
      }
    }
    return dataset;
  }
  
  public Dataset getByProjectAndFullPath(Project project, String fullPath) throws DatasetException {
    Inode inode = inodeController.getInodeAtPath(fullPath);
    if (project == null || inode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "path: " + fullPath);
    }
    Dataset dataset = datasetFacade.findByProjectAndInode(project, inode);
    if (dataset == null) { // not owned by project check shared
      dataset = datasetFacade.findByInode(inode);
      if (dataset == null) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "path: " + fullPath);
      }
      DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(project, dataset);
      if (datasetSharedWith == null) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "path: " + fullPath);
      }
      return datasetSharedWith.getDataset();
    }
    return dataset;
  }
  
  public FilePreviewDTO filePreview(Project project, Users user, Path fullPath, FilePreviewMode mode,
    List<String> allowedImgExtension) throws DatasetException {
    String username = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;
    FSDataInputStream is = null;
    FilePreviewDTO filePreviewDTO = null;
    String fileName = fullPath.getName();
    try {
      udfso = dfs.getDfsOps(username);
      //tests if the user have permission to access this path
      is = udfso.open(fullPath);
      //Get file type first. If it is not a known image type, display its
      //binary contents instead
      String fileExtension = "txt"; // default file  type
      //Check if file contains a valid extension
      if (fileName.contains(".")) {
        fileExtension = fileName.substring(fileName.lastIndexOf(".")).replace(".", "").toUpperCase();
      }
      long fileSize = udfso.getFileStatus(fullPath).getLen();
      if (allowedImgExtension.contains(fileExtension)) {
        //If it is an image smaller than 10MB download it otherwise thrown an error
        if (fileSize < settings.getFilePreviewImageSize()) {
          //Read the image in bytes and convert it to base64 so that is
          //rendered properly in the front-end
          byte[] imageInBytes = new byte[(int) fileSize];
          is.readFully(imageInBytes);
          String base64Image = new Base64().encodeAsString(imageInBytes);
          filePreviewDTO = new FilePreviewDTO(Settings.FILE_PREVIEW_IMAGE_TYPE, fileExtension.toLowerCase(),
            base64Image);
        } else {
          throw new DatasetException(RESTCodes.DatasetErrorCode.IMAGE_SIZE_INVALID, Level.FINE);
        }
      } else {
        try (DataInputStream dis = new DataInputStream(is)) {
          int sizeThreshold = Settings.FILE_PREVIEW_TXT_SIZE_BYTES; //in bytes
          if (fileSize > sizeThreshold && !fileName.endsWith(Settings.README_FILE) &&
            mode.equals(FilePreviewMode.TAIL)) {
            dis.skipBytes((int) (fileSize - sizeThreshold));
          } else if (fileName.endsWith(Settings.README_FILE) && fileSize > Settings.FILE_PREVIEW_TXT_SIZE_BYTES) {
            throw new DatasetException(RESTCodes.DatasetErrorCode.FILE_PREVIEW_ERROR, Level.FINE,
              "File must be smaller than " + Settings.FILE_PREVIEW_TXT_SIZE_BYTES / 1024 + " KB to be previewed");
          } else if ((int) fileSize < sizeThreshold) {
            sizeThreshold = (int) fileSize;
          }
          byte[] headContent = new byte[sizeThreshold];
          dis.readFully(headContent, 0, sizeThreshold);
          //File content
          filePreviewDTO = new FilePreviewDTO(Settings.FILE_PREVIEW_TEXT_TYPE, fileExtension.toLowerCase(),
            new String(headContent));
        }
      }
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.SEVERE, "path: " +
        fullPath.toString(), ex.getMessage(), ex);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          LOGGER.log(Level.SEVERE, "Error while closing stream.", ex);
        }
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    return filePreviewDTO;
  }
  
  /**
   * Checks if a path exists. Will require a read access to the path.
   * @param filePath
   * @param username
   * @throws DatasetException
   */
  public void checkFileExists(Path filePath, String username) throws DatasetException {
    DistributedFileSystemOps udfso = null;
    boolean exist;
    try {
      udfso = dfs.getDfsOps(username);
      exist = udfso.exists(filePath);
    } catch (AccessControlException ae) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_ACCESS_PERMISSION_DENIED, Level.FINE,
        "path: " + filePath.toString(), ae.getMessage(), ae);
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_NOT_FOUND, Level.FINE, "path: " +
        filePath.toString(), ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    if (!exist) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_NOT_FOUND, Level.FINE,
        "path: " + filePath.toString());
    }
  }
  
  public void unzip(Project project, Users user, Path path) throws DatasetException {
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    checkFileExists(path, hdfsUser);
    String localDir = DigestUtils.sha256Hex(path.toString());
    String stagingDir = settings.getStagingDir() + File.separator + localDir;
    
    File unzipDir = new File(stagingDir);
    unzipDir.mkdirs();
    settings.addUnzippingState(path.toString());
    
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand(settings.getHopsworksDomainDir() + "/bin/unzip-background.sh")
      .addCommand(stagingDir)
      .addCommand(path.toString())
      .addCommand(hdfsUser)
      .ignoreOutErrStreams(true)
      .build();
    
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      int result = processResult.getExitCode();
      if (result == 2) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_SIZE_ERROR, Level.WARNING);
      }
      if (result != 0) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_ERROR, Level.WARNING,
          "path: " + path.toString() + ", result: " + result);
      }
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_ERROR, Level.SEVERE,
        "path: " + path.toString(), ex.getMessage(), ex);
    }
  }
  
  public void zip(Project project, Users user, Path path) throws DatasetException {
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    checkFileExists(path, hdfsUser);
    String localDir = DigestUtils.sha256Hex(path.toString());
    String stagingDir = settings.getStagingDir() + File.separator + localDir;
    
    File zipDir = new File(stagingDir);
    zipDir.mkdirs();
    settings.addZippingState(path.toString());
    
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand(settings.getHopsworksDomainDir() + "/bin/zip-background.sh")
      .addCommand(stagingDir)
      .addCommand(path.toString())
      .addCommand(hdfsUser)
      .ignoreOutErrStreams(true)
      .build();
    
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      int result = processResult.getExitCode();
      if (result == 2) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_SIZE_ERROR, Level.WARNING);
      }
      if (result != 0) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_ERROR, Level.WARNING,
          "path: " + path.toString() + ", result: " + result);
      }
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_ERROR, Level.SEVERE,
        "path: " + path.toString(), ex.getMessage(), ex);
    }
  }
  
  public void share(String targetProjectName, String fullPath, Project project, Users user)
    throws DatasetException, ProjectException {
    Project targetProject = projectFacade.findByName(targetProjectName);
    Dataset ds = getByProjectAndFullPath(project, fullPath);
    if (targetProject == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "Target project not found.");
    }
    DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(targetProject, ds);
    if (datasetSharedWith != null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
        "Dataset already in " + targetProject.getName());
    }
    // Create the new Dataset entry
    datasetSharedWith = new DatasetSharedWith(targetProject, ds, true);
    // if the dataset is not requested or is requested by a data scientist set status to pending.
    DatasetRequest dsReq = datasetRequest.findByProjectAndDataset(targetProject, ds);
    if (dsReq == null || dsReq.getProjectTeam().getTeamRole().equals(AllowedRoles.DATA_SCIENTIST)) {
      datasetSharedWith.setAccepted(false);
    } else {
      hdfsUsersController.shareDataset(targetProject, ds);
    }
    datasetSharedWithFacade.save(datasetSharedWith);
    if (dsReq != null) {
      datasetRequest.remove(dsReq);//the dataset is shared so remove the request.
    }
    
    activityFacade
      .persistActivity(ActivityFacade.SHARED_DATA + ds.getName() + " with project " + targetProject.getName(),
        project, user, ActivityFlag.DATASET);
  }
  
  public void acceptShared(Project project, DatasetSharedWith datasetSharedWith) throws DatasetException {
    if (datasetSharedWith == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
    }
    hdfsUsersController.shareDataset(project, datasetSharedWith.getDataset());
    datasetSharedWith.setAccepted(true);
    datasetSharedWithFacade.update(datasetSharedWith);
    
  }
  
  public void rejectShared(DatasetSharedWith datasetSharedWith) throws DatasetException {
    if (datasetSharedWith == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
    }
    datasetSharedWithFacade.remove(datasetSharedWith);
  }
  
  public void createDirectory(Project project, Users user, Path fullPath, String name, Boolean isDataset,
    Integer templateId, String description, Boolean searchable, Boolean generateReadme) throws DatasetException,
    HopsSecurityException {
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    String username = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(username);
    if (templateId == null) {
      templateId = -1;
    }
    if (searchable == null) {
      searchable = false;
    }
    if (description == null) {
      description = "";
    }
    try {
      if (isDataset) {
        createDataset(user, project, name, description, templateId, searchable, false, false, dfso);
        //Generate README.md for the dataset if the user requested it
        if (generateReadme != null && generateReadme) {
          //Persist README.md to hdfs
          generateReadme(udfso, name, description, project.getName());
        }
      } else {
        createSubDirectory(project, fullPath, templateId, description, searchable, udfso);
      }
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void move(Project project, Users user, Path sourcePath, Path destPath, Dataset sourceDataset,
    Dataset destDataset) throws DatasetException, HopsSecurityException {
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    if (!getOwningProject(sourceDataset).equals(destDataset.getProject())) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_FORBIDDEN, Level.FINE,
        "Cannot copy file/folder from another project.");
    }
    if (destDataset.isPublicDs()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_FORBIDDEN, Level.FINE,
        "Can not move to a public dataset.");
    }
    DistributedFileSystemOps udfso = null;
    //We need super-user to change owner
    DistributedFileSystemOps dfso = null;
    try {
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = getOwningProject(sourceDataset);
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && projectTeamFacade.findCurrentRole(owning, user).equals(AllowedRoles.DATA_OWNER) &&
        owning.equals(project)) {
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
      recChangeOwnershipAndPermission(destPath, permission, owner, group, dfso, udfso);
    } catch (AccessControlException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.HDFS_ACCESS_CONTROL, Level.FINE,
        "Operation: move, from: " + sourcePath.toString() + " to: " + destPath.toString());
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.SEVERE,
        "move operation failed for: " + sourcePath.toString(), ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
      if (dfso != null) {
        dfso.close();
      }
    }
  }
  
  public void copy(Project project, Users user, Path sourcePath, Path destPath, Dataset sourceDataset,
    Dataset destDataset) throws DatasetException {
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    if (!getOwningProject(sourceDataset).equals(destDataset.getProject())) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.COPY_FROM_PROJECT, Level.FINE);
    }
    if (destDataset.isPublicDs()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.COPY_TO_PUBLIC_DS, Level.FINE);
    }
    
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(username);
      if (udfso.exists(destPath.toString())) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE);
      }
      //Get destination folder permissions
      FsPermission permission = udfso.getFileStatus(destPath.getParent()).getPermission();
      udfso.copyInHdfs(sourcePath, destPath);
      //Set permissions
      recChangeOwnershipAndPermission(destPath, permission, null, null, null, udfso);
      
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.SEVERE, "move operation " +
        "failed for: " + sourcePath.toString(), ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void setPermissions(Path path, Dataset ds, DatasetPermissions datasetPermissions, Project project, Users user)
    throws DatasetException {
    if (ds.isShared(project) || (ds.isPublicDs() && !ds.isShared(project))) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OWNER_ERROR, Level.FINE);
    }
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      // Change permission as super user
      FsPermission fsPermission = null;
      if (null != datasetPermissions) {
        fsPermission = datasetPermissions.toFsPermission();
        recChangeOwnershipAndPermission(path, fsPermission, null, null, null, dfso);
      }
    } catch (IOException e) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_PERMISSION_ERROR, Level.WARNING,
        "dataset: " + ds.getId(), e.getMessage(), e);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }
  
  public void delete(Project project, Users user, Path fullPath, Dataset dataset, boolean isDataset)
    throws DatasetException {
    boolean success;
    String username = hdfsUsersController.getHdfsUserName(project, user);
    Project owning = getOwningProject(dataset);
    DistributedFileSystemOps dfso = null;
    if (isDataset && dataset.isShared(project)) {
      // The user is trying to delete a dataset. Drop it from the table
      // But leave it in hopsfs because the user doesn't have the right to delete it
      hdfsUsersController.unShareDataset(project, dataset);
      datasetFacade.removeDataset(dataset);
    } else {
      try {
        //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
        //Find project of dataset as it might be shared
        boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
        if (isMember && projectTeamFacade.findCurrentRole(owning, user).equals(AllowedRoles.DATA_OWNER) &&
          owning.equals(project)) {
          dfso = dfs.getDfsOps();// do it as super user
        } else {
          dfso = dfs.getDfsOps(username);// do it as project user
        }
        if (isDataset) {
          success = deleteDatasetDir(dataset, fullPath, dfso);
        } else {
          success = dfso.rm(fullPath, true);
        }
      } catch (AccessControlException ae) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_ACCESS_PERMISSION_DENIED, Level.FINE,
          "path: " + fullPath.toString(), ae.getMessage(), ae);
      } catch (FileNotFoundException fnfe) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_NOT_FOUND, Level.FINE,
          "path: " + fullPath.toString(), fnfe.getMessage(), fnfe);
      } catch (IOException ex) {
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
      
      if (isDataset) {
        //remove the group associated with this dataset as it is a toplevel ds
        try {
          hdfsUsersController.deleteDatasetGroup(dataset);
        } catch (IOException ex) {
          //FIXME: take an action?
          LOGGER.log(Level.WARNING, "Error while trying to delete a dataset group", ex);
        }
      }
    }
  }
  
  public void deleteCorrupted(Project project, Users user, Path fullPath, Dataset dataset) throws DatasetException {
    DistributedFileSystemOps dfso = null;
    try {
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = getOwningProject(dataset);
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && owning.equals(project)) {
        dfso = dfs.getDfsOps();// do it as super user
        FileStatus fs = dfso.getFileStatus(fullPath);
        String owner = fs.getOwner();
        long len = fs.getLen();
        if (owner.equals(settings.getHopsworksUser()) && len == 0) {
          dfso.rm(fullPath, true);
        }
      }
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.SEVERE,
        "path: " + fullPath.toString(), ex.getMessage(), ex);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }
  
  public void unshare(Project project, Users user, Dataset dataset, String targetProjectName) throws DatasetException {
    Project proj = projectFacade.findByName(targetProjectName);
    DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(proj, dataset);
    if (datasetSharedWith == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_SHARED_WITH_PROJECT, Level.FINE,
        "project: " + proj.getName());
    }
    hdfsUsersController.unshareDataset(proj, dataset);
    datasetSharedWithFacade.remove(datasetSharedWith);
    activityFacade.persistActivity(ActivityFacade.UNSHARED_DATA + dataset.getName() + " with project " +
      proj.getName(), project, user, ActivityFlag.DATASET);
  }
  
  public void updateDescription(Project project, Users user, Dataset dataset, String description) {
    if (description != null && !dataset.getDescription().equals(description)) {
      dataset.setDescription(description);
      datasetFacade.update(dataset);
      activityFacade.persistActivity(ActivityFacade.UPDATE_DATASET_DESCRIPTION + dataset.getName(), project, user,
        ActivityFlag.DATASET);
    }
  }
}
