package io.hops.hopsworks.common.dataset;

import io.hops.common.Pair;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
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
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;

/**
 * Contains business logic pertaining DataSet management.
 * <p>
 */
@Stateless
public class DatasetController {

  private static final Logger LOGGER = Logger.getLogger(DatasetController.class.getName());
  @EJB
  private InodeFacade inodes;
  @EJB
  private TemplateFacade templates;
  @EJB
  private DatasetFacade datasetFacade;
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
   * @param defaultDataset
   * @param dfso
   * @throws NullPointerException If any of the given parameters is null.
   * @throws io.hops.hopsworks.common.exception.AppException
   * @throws IllegalArgumentException If the given DataSetDTO contains invalid
   * folder names, or the folder already exists.
   * @throws IOException if the creation of the dataset failed.
   * @see FolderNameValidator.java
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void createDataset(Users user, Project project, String dataSetName,
      String datasetDescription, int templateId, boolean searchable,
      boolean defaultDataset, DistributedFileSystemOps dfso)
      throws IOException, AppException {
    //Parameter checking.
    if (user == null) {
      throw new NullPointerException(
          "A valid user must be passed upon DataSet creation. Received null.");
    } else if (project == null) {
      throw new NullPointerException(
          "A valid project must be passed upon DataSet creation. Received null.");
    } else if (dataSetName == null) {
      throw new NullPointerException(
          "A valid DataSet name must be passed upon DataSet creation. Received null.");
    }
    try {
      FolderNameValidator.isValidName(dataSetName, false);
    } catch (ValidationException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Invalid folder name for DataSet: " + e.getMessage());
    }
    //Logic
    boolean success;
    String dsPath = File.separator + Settings.DIR_ROOT + File.separator
        + project.getName();
    dsPath = dsPath + File.separator + dataSetName;
    Inode parent = inodes.getProjectRoot(project.getName());
    Inode ds = inodes.findByInodePK(parent, dataSetName,
        HopsUtils.dataSetPartitionId(parent, dataSetName));

    if (ds != null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Invalid folder name for DataSet: "
          + ResponseMessages.FOLDER_NAME_EXIST);
    }
    //Permission 770
    FsAction global = FsAction.NONE;
    FsAction group = (defaultDataset ? FsAction.ALL
        : FsAction.READ_EXECUTE);
    FsPermission fsPermission = new FsPermission(FsAction.ALL,
        group, global, defaultDataset);
    success = createFolder(dsPath, templateId, fsPermission, dfso);
    if (success) {
      try {
        ds = inodes.findByInodePK(parent, dataSetName,
            HopsUtils.dataSetPartitionId(parent, dataSetName));
        Dataset newDS = new Dataset(ds, project);
        newDS.setSearchable(searchable);

        if (datasetDescription != null) {
          newDS.setDescription(datasetDescription);
        }
        datasetFacade.persistDataset(newDS);
        activityFacade.persistActivity(ActivityFacade.NEW_DATA + dataSetName,
            project, user);
        // creates a dataset and adds user as owner.
        hdfsUsersBean.addDatasetUsersGroups(user, project, newDS, dfso);

        //set the dataset meta enabled. Support 3 level indexing
        if (searchable) {
          dfso.setMetaEnabled(dsPath);
          Dataset logDs = datasetFacade.findByNameAndProjectId(project, dataSetName);
          logDataset(logDs, OperationType.Add);
        }
      } catch (Exception e) {
        IOException failed = new IOException("Failed to create dataset at path "
            + dsPath + ".", e);
        try {
          dfso.rm(new Path(dsPath), true);//if dataset persist fails rm ds folder.
          throw failed;
        } catch (IOException ex) {
          throw new IOException(
              "Failed to clean up properly on dataset creation failure", ex);
        }
      }
    } else {
      throw new IOException("Could not create the directory at " + dsPath);
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
   * @throws java.io.IOException If something goes wrong upon the creation of
   * the directory.
   * @throws io.hops.hopsworks.common.exception.AppException
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
  public void createSubDirectory(Project project, Path dirPath,
      int templateId, String description, boolean searchable,
      DistributedFileSystemOps udfso) throws IOException, AppException {

    if (project == null) {
      throw new NullPointerException(
          "Cannot create a directory under a null project.");
    } else if (dirPath == null) {
      throw new NullPointerException(
          "Cannot create a directory for an empty path.");
    }

    String folderName = dirPath.getName();
    String parentPath = dirPath.getParent().toString();
    try {
      FolderNameValidator.isValidName(folderName, true);
    } catch (ValidationException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.getLocalizedMessage());
    }

    //Check if the given folder already exists
    if (inodes.existsPath(dirPath.toString())) {
      throw new IllegalArgumentException("The given path already exists.");
    }

    // Check if the parent directory exists
    Inode parent = inodes.getInodeAtPath(parentPath);
    if (parent == null) {
      throw new IllegalArgumentException(
          "Path for parent folder does not exist: "
          + parentPath + " under " + project.getName());
    }

    //Now actually create the folder
    boolean success = this.createFolder(dirPath.toString(), templateId,
        null, udfso);

    //if the folder was created successfully, persist basic metadata to it -
    //description and searchable attribute
    if (success) {
      //find the corresponding inode
      int partitionId = HopsUtils.calculatePartitionId(parent.getId(),
          folderName, dirPath.depth());
      Inode folder = this.inodes.findByInodePK(parent, folderName, partitionId);
      InodeBasicMetadata basicMeta = new InodeBasicMetadata(folder, description,
          searchable);
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
    boolean success;
    success = udfso.rm(location, true);
    if (success) {
      operationsLogFacade.persist(log);
    }
    return success;
  }

  /**
   * Change "editability" of all the datasets related to the same
   * original dataset
   *
   * @param orgDs the dataset to be make editable
   * @param editable whether the dataset should be editable
   */
  //TODO: Add a reference in each dataset entry to the original dataset
  public void changeEditable(Dataset orgDs, boolean editable) {
    for (Dataset ds : datasetFacade.findByInode(orgDs.getInode())) {
      ds.setEditable(editable);
      datasetFacade.merge(ds);
    }
  }

  public void recChangeOwnershipAndPermission(Path path, FsPermission permission,
      String username, String group,
      DistributedFileSystemOps dfso,
      DistributedFileSystemOps udfso)
      throws IOException {

    /*
     * TODO: Currently there is no change permission recursively operation
     * available in HopsFS client. So we build all the path of the tree and
     * we call the set permission on each one
     */
    // Set permission/ownership for the root
    if (username != null && group != null && dfso != null) {
      dfso.setOwner(path, username, group);
    }
    udfso.setPermission(path, permission);
    Inode rootInode = inodes.getInodeAtPath(path.toString());

    // Keep a list of directories to avoid using recursion
    // Remember also the path to avoid going to the database for path resolution
    Stack<Pair<Inode, Path>> dirs = new Stack<>();
    if (rootInode.isDir()) {
      dirs.push(new Pair<>(rootInode, path));
    }

    while (!dirs.isEmpty()) {
      Pair<Inode, Path> dirInode = dirs.pop();
      for (Inode child : inodes.getChildren(dirInode.getL())) {
        Path childPath = new Path(dirInode.getR(), child.getInodePK().getName());

        if (username != null && group != null && dfso != null) {
          dfso.setOwner(path, username, group);
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
      DistributedFileSystemOps dfso) throws IOException {
    boolean success = false;
    Path location = new Path(path);
    if (fsPermission == null) {
      fsPermission = dfso.getParentPermission(location);
    }
    try {
      success = dfso.mkdir(location, fsPermission);
      if (success) {
        dfso.setPermission(location, fsPermission);
      }
      if (success && template != 0 && template != -1) {
        //Get the newly created Inode.
        Inode created = inodes.getInodeAtPath(path);
        Template templ = templates.findByTemplateId(template);
        if (templ != null) {
          templ.getInodes().add(created);
          //persist the relationship table
          templates.updateTemplatesInodesMxN(templ);
        }
      }
    } catch (AccessControlException ex) {
      throw new AccessControlException(ex);
    } catch (IOException ex) {
      throw new IOException("Could not create the directory at " + path, ex);
    } catch (DatabaseException e) {
      throw new IOException("Could not attach template to folder. ", e);
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
      readMeFilePath = "/Projects/" + project + "/" + dsName + "/README.md";

      try (FSDataOutputStream fsOut = udfso.create(readMeFilePath)) {
        fsOut.writeBytes(readmeFile);
        fsOut.flush();
        udfso.setPermission(new org.apache.hadoop.fs.Path(readMeFilePath),
            new FsPermission(FsAction.ALL,
                FsAction.READ_EXECUTE,
                FsAction.NONE));
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
      throws AccessControlException, IOException {
    if (path == null || dfso == null) {
      throw new IllegalArgumentException("One or more arguments are not set.");
    }
    if (!path.endsWith("README.md")) {
      throw new IllegalArgumentException("Path does not contain readme file.");
    }
    FilePreviewDTO filePreviewDTO = null;
    FSDataInputStream is;
    DataInputStream dis = null;
    try {
      if (!dfso.exists(path) || dfso.isDir(path)) {
        throw new IOException("The file does not exist");
      }
      is = dfso.open(path);
      dis = new DataInputStream(is);
      long fileSize = dfso.getFileStatus(new org.apache.hadoop.fs.Path(
          path)).getLen();
      if (fileSize > Settings.FILE_PREVIEW_TXT_SIZE_BYTES_README) {
        throw new IllegalArgumentException("README.md must be smaller than"
            + Settings.FILE_PREVIEW_TXT_SIZE_BYTES_README
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

  public void logDataset(Dataset dataset, OperationType type) {
    if (dataset.isShared() || !dataset.isSearchable()) {
      return;
    }
    operationsLogFacade.persist(new OperationsLog(dataset, type));
  }

  public Path getDatasetPath(Dataset ds) {
    Path path = null;
    switch (ds.getType()) {
      case DATASET:
        Project owningProject = getOwningProject(ds);
        path = new Path(Settings.getProjectPath(owningProject.getName()),
            ds.getInode().getInodePK().getName());
        break;
      case HIVEDB:
      // TODO (Fabio) - add hive dataset path resolution here
    }

    return path;
  }

  public Project getOwningProject(Dataset ds) {
    // If the dataset is not a shared one, just return the project
    if (!ds.isShared()) {
      return ds.getProject();
    }

    switch (ds.getType()) {
      case DATASET:
        // Get the owning project based on the dataset inode
        Inode projectInode = inodes.findParent(ds.getInode());
        return projectFacade.findByName(projectInode.getInodePK().getName());
      case HIVEDB:
        // TODO (Fabio) - add hive owner resolution here
        return null;
      default:
        return null;
    }
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
          udfso.close();
        }
      }
    }
    return false;
  }
}
