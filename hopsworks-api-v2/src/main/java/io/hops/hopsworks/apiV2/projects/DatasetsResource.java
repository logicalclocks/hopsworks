package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.SystemCommandExecutor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "Datasets")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatasetsResource {

  private final static Logger logger = Logger.getLogger(DatasetsResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private UserManager userBean;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private InodeFacade inodes;
  @EJB
  private UserFacade userFacade;
  @EJB
  private JobController jobController;
  @EJB
  private AsynchronousJobExecutor async;
  @EJB
  private YarnJobsMonitor jobsMonitor;
  @Inject
  private BlobsResource blobsResource;
  @EJB
  private Settings settings;

  private Project project;
  
  public void setProject(Integer projectId) throws AppException {
    this.project = projectFacade.find(projectId);
    if(project == null){
      throw new AppException(Response.Status.NOT_FOUND, ResponseMessages.PROJECT_NOT_FOUND);
    }
  }
  
  @ApiOperation(value = "Get a list of datasets in project", notes = "Returns a list of project datasets, and " +
      "datasets that are shared the project.")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getDataSets( @Context SecurityContext sc){
    
    List<DatasetView> dsViews = new ArrayList<>();
    for (Dataset dataset : project.getDatasetCollection()){
      dsViews.add(new DatasetView(dataset));
    }
    
    GenericEntity<List<DatasetView>> result = new GenericEntity<List<DatasetView>>(dsViews) {};
    return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation(value = "Create a dataset", notes = "Create a dataset in this project.")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response createDataSet(CreateDatasetView createDatasetView, @Context SecurityContext sc,
      @Context HttpServletRequest req, @Context UriInfo uriInfo) throws AppException {
    
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    if (username == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "User not found");
    }
    DistributedFileSystemOps udfso = dfs.getDfsOps(username);
    
    try {
      datasetController.createDataset(user, project, createDatasetView.getName(),
          createDatasetView.getDescription(), createDatasetView.getTemplate(), createDatasetView.isSearchable(),
          false, dfso); // both are dfso to create it as root user
      
      //Generate README.md for the dataset if the user requested it
      if (createDatasetView.isGenerateReadme()) {
        //Persist README.md to hdfs
        datasetController.generateReadme(udfso, createDatasetView.getName(), createDatasetView.getDescription(),
            project.getName());
      }
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "Failed to create dataset: " + e.getLocalizedMessage());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    Dataset dataset = getDataset(createDatasetView.getName());
    GenericEntity<DatasetView> created = new GenericEntity<DatasetView>(new DatasetView(dataset)){};
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    builder.path(dataset.getName());
    return Response.created(builder.build()).type(MediaType.APPLICATION_JSON_TYPE).entity(created).build();
  }
  
  @ApiOperation(value = "Get dataset metadata", notes = "Get information about a project's dataset.")
  @GET
  @Path("/{dsName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getDataset(@PathParam("dsName") String name, @Context
      SecurityContext sc) throws AppException {
    
    Dataset toReturn = getDataset(name);
    GenericEntity<DatasetView> dsView = new GenericEntity<DatasetView>(new DatasetView(toReturn)){};
    
    return Response.ok(dsView,MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  private Dataset getDataset(String name) throws AppException {
    Dataset byNameAndProjectId =
        datasetFacade.findByNameAndProjectId(project, name);
    if (byNameAndProjectId == null){
      throw new AppException(Response.Status.NOT_FOUND, "dataset with name" + name + " can not be found.");
    }
    
    return byNameAndProjectId;
  }
  
  /**
   * This function is used only for deletion of dataset directories
   * as it does not accept a path
   * @param name
   * @param sc
   * @param req
   * @return
   * @throws io.hops.hopsworks.common.exception.AppException
   * @throws org.apache.hadoop.security.AccessControlException
   */
  @ApiOperation(value = "Delete dataset", notes = "Delete a dataset and all its files. Only allowed for data-owners.")
  @DELETE
  @Path("/{dsName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteDataSet(@PathParam("dsName") String name, @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException, AccessControlException {
    
    Dataset dataset = getDataset(name);

    if (dataset.isShared()) {
      // The user is trying to delete a dataset. Drop it from the table
      // But leave it in hopsfs because the user doesn't have the right to delete it
      hdfsUsersBean.unShareDataset(project, dataset);
      datasetFacade.removeDataset(dataset);
      return Response.noContent().build();
    }
  
    org.apache.hadoop.fs.Path fullPath = pathValidator.getFullPath(new DatasetPath(dataset, "/"));
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    DistributedFileSystemOps dfso = getDfsOpsForUserHelper(dataset, user);
    boolean success;
    try {
      success = datasetController.deleteDatasetDir(dataset, fullPath, dfso);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You can not delete the file " + fullPath.toString());
    } catch (IOException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not delete the file at " + fullPath.toString());
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
    
    if (!success) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not delete the file at " + fullPath.toString());
    }
    
    //remove the group associated with this dataset as it is a toplevel ds
    try {
      hdfsUsersBean.deleteDatasetGroup(dataset);
    } catch (IOException ex) {
      //FIXME: take an action?
      logger.log(Level.WARNING, "Error while trying to delete a dataset group", ex);
    }
    return Response.noContent().build();
  }
  
  private DistributedFileSystemOps getDfsOpsForUserHelper(Dataset ds, Users user){
    DistributedFileSystemOps dfso;
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    
    //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
    //Find project of dataset as it might be shared
    Project owning = datasetController.getOwningProject(ds);
    boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
    if (isMember && projectTeamFacade.findCurrentRole(owning, user).equals(AllowedProjectRoles.DATA_OWNER)
        && owning.equals(project)) {
      dfso = dfs.getDfsOps();// do it as super user
    } else {
      dfso = dfs.getDfsOps(username);// do it as project user
    }
    return dfso;
  }
  
  @ApiOperation(value = "Get dataset README-file")
  @GET
  @Path("/{dsName}/readme")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getReadme(@PathParam("dsName") String datasetName, @Context SecurityContext sc)
      throws AppException, AccessControlException {
    return getFileOrDir(datasetName, "README.md", sc);
  }
  
  @ApiOperation("Get a list of projects that share this dataset")
  @GET
  @Path("/{dsName}/projects")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSharingProjects(
      @PathParam("dsName") String name,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException,
      AccessControlException {
    Dataset ds = getDataset(name);
    
    List<Project> list = datasetFacade.findProjectSharedWith(project, ds.getInode());
    List<ProjectView> projectViews = new ArrayList<>();
    for (Project project : list) {
      projectViews.add(new ProjectView(project));
    }
    GenericEntity<List<ProjectView>> projects = new GenericEntity<List<ProjectView>>(projectViews) { };
    
    return Response.ok(projects, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation(value = "Check if dataset editable", notes = "Data scientists are allowed to create files in editable" +
      " datasets.")
  @GET
  @Path("/{dsName}/editable")
  public Response isEditable(@PathParam("dsName") String name, @Context SecurityContext sc) throws AppException {
    Dataset ds = getDataset(name);
    if (ds.isEditable()){
      return Response.noContent().build();
    } else {
      throw new AppException(Response.Status.NOT_FOUND, "Dataset not readonly");
    }
  }
  
  @ApiOperation(value = "Make dataset editable", notes = "Allow data scientists to create and modify own " +
      "files in dataset.")
  @PUT
  @Path("/{dsName}/editable")
  public Response makeEditable(@PathParam("dsName") String name) throws AppException, AccessControlException {
    Dataset dataSet = getDataset(name);
    FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.NONE, true);
    changeDatasetPermissions(dataSet, fsPermission);
    datasetController.changeEditable(dataSet, true);
    
    return Response.noContent().build();
  }
  
  @ApiOperation(value = "Make dataset non-editable", notes = "Disallow data scientists creating files in dataset.")
  @DELETE
  @Path("/{dsName}/editable")
  public Response makeNonEditable(@PathParam("dsName") String name) throws AppException, AccessControlException {
    Dataset dataset = getDataset(name);
    FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.READ_EXECUTE, FsAction.NONE, false);
    changeDatasetPermissions(dataset, fsPermission);
    datasetController.changeEditable(dataset, false);
  
    return Response.noContent().build();
  }
  
  private void changeDatasetPermissions(Dataset dataset, FsPermission fsPermission)
      throws AccessControlException, AppException {
    DistributedFileSystemOps dfso = null;
    try {
      // change the permissions as superuser
      dfso = dfs.getDfsOps();
      datasetController.recChangeOwnershipAndPermission(datasetController.getDatasetPath(dataset),
          fsPermission, null, null, null, dfso);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: Can not change the permission of this file.");
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "Error while creating directory: " + e.
          getLocalizedMessage());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }
  
  //File operations
  @ApiOperation(value = "Get dataset file/dir listing", notes = "Returns metadata of " +
      "the files and folders in the dataset root.")
  @GET
  @Path("/{dsName}/files")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getDatasetRoot(@PathParam("dsName") String name, @Context SecurityContext sc) throws AppException {
    Dataset dataset = getDataset(name);
    DatasetPath path = new DatasetPath(dataset, "/");
  
    String fullPath = pathValidator.getFullPath(path).toString();
  
    Inode inode = pathValidator.exists(path, inodes, true);
  
    GenericEntity<List<InodeView>> entity =
        getDirHelper(inode, fullPath, dataset.isShared());
    return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation(value = "Get a listing for a path in a dataset", notes = "Returns metadata of the files and folders " +
      "on the specified path.")
  @GET
  @Path("/{dsName}/files/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getFileOrDir(@PathParam("dsName") String name,
      @PathParam ("path") String relativePath,
      @Context SecurityContext sc ) throws
      AppException, AccessControlException {
    
    Dataset dataSet = getDataset(name);
    DatasetPath path = new DatasetPath(dataSet, relativePath);
    String fullPath = pathValidator.getFullPath(path).toString();
    
    Inode inode = pathValidator.exists(path, inodes, null);
    
    if (inode.isDir()){
      GenericEntity<List<InodeView>> entity = getDirHelper(inode, fullPath, dataSet.isShared());
      return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build();
    } else {
      GenericEntity<InodeView> entity = getFileHelper(inode, fullPath);
      return Response.ok(entity,MediaType.APPLICATION_JSON_TYPE).build();
    }
  }
  
  @ApiOperation(value = "Delete a file or directory", notes = "Delete a file or directory from the dataset.")
  @DELETE
  @Path("/{dsName}/files/{path: .+}")
  public Response deleteFileOrDir(@PathParam("dsName") String datasetName, @PathParam("path") String path, @Context
      SecurityContext sc, @Context HttpServletRequest req) throws AccessControlException, AppException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    Dataset dataset = getDataset(datasetName);
    DistributedFileSystemOps dfso = getDfsOpsForUserHelper(dataset, user);
    org.apache.hadoop.fs.Path fullPath = pathValidator.getFullPath(new DatasetPath(dataset, path));
  
    boolean success;
    try {
      success = dfso.rm(fullPath, true);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You can not delete the file " + fullPath);
    } catch (IOException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not delete the file at " + fullPath);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
    if (!success) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not delete the file at " + fullPath);
    }
    
    return Response.noContent().build();
  }
  
  @PUT
  @Path("/{dsName}/files/{path: .+}")
  @ApiOperation(value = "Copy, Move", notes = "Performs the selected operation on the file/dir " +
      "specified in the src parameter. Data cannot cross project boundary.")
  public Response copyMove(@PathParam("dsName") String targetDataset,
      @PathParam("path") String targetPath,
      @ApiParam(allowableValues = "copy,move", required = true, value = "\"copy\" or \"move\"")
      @QueryParam("op") String operation,
      @ApiParam(required = true, value = "Path to source file in dataset")
      @QueryParam("src") String sourcePath,
      @ApiParam("Name of different source dataset if applicable.") @QueryParam("srcDsName") String sourceDataset,
      @Context SecurityContext sc, @Context HttpServletRequest req) throws AppException, AccessControlException {
    
    if (operation == null || !operation.matches("copy|move")){
      throw new AppException(Response.Status.BAD_REQUEST, "?op= parameter required, possible options: " +
          "copy|move");
    }
    
    if (sourcePath == null){
      throw new AppException(Response.Status.BAD_REQUEST, "?src= parameter required.");
    }
    
    Dataset targetDs = getDataset(targetDataset);
    if (targetDs.isPublicDs()){
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Target dataset is public. Public datasets " +
          "are immutable.");
    }
  
    DatasetPath sourceDatasetPath;
    if (sourceDataset != null){
      Dataset sourceDs = getDataset(sourceDataset);
      if (!project.equals(datasetController.getOwningProject(sourceDs))){
        throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
            "Cannot "+ operation + " file/folder across projects.");
      }
      sourceDatasetPath = new DatasetPath(sourceDs, sourcePath);
    } else {
      //source inside target dataset
      sourceDatasetPath = new DatasetPath(targetDs, sourcePath);
    }
    
    DatasetPath targetDatasetPath = new DatasetPath(targetDs, targetPath);
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    switch(operation){
      case "copy":
        return copyHelper(user, sourceDatasetPath, targetDatasetPath);
      case "move":
        return moveHelper(user, sourceDatasetPath, targetDatasetPath);
      default:
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR, ResponseMessages.INTERNAL_SERVER_ERROR);
    }
  }
  
  private Response copyHelper(Users user, DatasetPath src, DatasetPath dst)
      throws AppException, AccessControlException {
    String hdfsUserName = hdfsUsersBean.getHdfsUserName(project, user);
    
    org.apache.hadoop.fs.Path sourcePath = pathValidator.getFullPath(src);
    org.apache.hadoop.fs.Path destPath = pathValidator.getFullPath(dst);
  
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUserName);
    
      if (udfso.exists(destPath.toString())){
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            "Destination already exists.");
      }
    
      //Get destination folder permissions
      FsPermission permission = udfso.getFileStatus(destPath.getParent()).getPermission();
      udfso.copyInHdfs(sourcePath, destPath);
    
      //Set permissions
      datasetController.recChangeOwnershipAndPermission(destPath, permission,
          null, null, null, udfso);
    
      return Response.noContent().build();
    
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Copy at path:" + destPath.toString()
              + " failed. It is not a directory or you do not have permission to "
              + "do this operation");
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  private Response moveHelper(Users user, DatasetPath src, DatasetPath dst)
      throws AppException, AccessControlException {
  
    String hdfsUserName = hdfsUsersBean.getHdfsUserName(project, user);
    org.apache.hadoop.fs.Path sourcePath = pathValidator.getFullPath(src);
    org.apache.hadoop.fs.Path destPath = pathValidator.getFullPath(dst);
  
    DistributedFileSystemOps udfso = null;
    //We need super-user to change owner
    DistributedFileSystemOps dfso = null;
    try {
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = datasetController.getOwningProject(src.getDataSet());
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && projectTeamFacade.findCurrentRole(owning, user)
          .equals(AllowedProjectRoles.DATA_OWNER) && owning.equals(project)) {
        udfso = dfs.getDfsOps();// do it as super user
      } else {
        udfso = dfs.getDfsOps(hdfsUserName);// do it as project user
      }
      dfso = dfs.getDfsOps();
      if (udfso.exists(destPath.toString())) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            "Destination already exists.");
      }
      
      //Get destination folder permissions
      FsPermission permission = udfso.getFileStatus(destPath.getParent()).getPermission();
      String group = udfso.getFileStatus(destPath.getParent()).getGroup();
      String owner = udfso.getFileStatus(sourcePath).getOwner();
    
      udfso.moveWithinHdfs(sourcePath, destPath);
    
      // Change permissions recursively
      datasetController.recChangeOwnershipAndPermission(destPath, permission,
          owner, group, dfso, udfso);
    
      return Response.noContent().build();
    
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Move at path:" + destPath.toString() + " failed. It is not a directory or you do not have permission to"
              + " do this operation");
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
      if (dfso != null) {
        dfso.close();
      }
    }
  }
  
  @POST
  @Path("/{dsName}/files/{path: .+}")
  @ApiOperation(value = "Unzip", notes = "Asynchronously zips or unzips a folder in the dataset.")
  public Response unzip(@PathParam("dsName") String targetDataset,
      @ApiParam("Path to folder") @PathParam("path") String targetPath,
      @ApiParam(value = "\"unzip\"", allowableValues = "unzip", required = true)
      @QueryParam("op") String operation,
      @Context SecurityContext sc, @Context HttpServletRequest req) throws AppException, AccessControlException {
    Dataset dataset = getDataset(targetDataset);
    
    if (operation == null || !operation.matches("unzip")){
      throw new AppException(Response.Status.BAD_REQUEST, "Must supply ?op= as \"unzip\"");
    }
    
    switch (operation){
      case "unzip":
        return unzip(dataset, targetPath, sc);
      default:
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR, ResponseMessages.INTERNAL_SERVER_ERROR);
    }
  }
  
  private Response unzip(Dataset dataset, String targetPath, SecurityContext sc)
      throws AppException, AccessControlException {
    DatasetPath datasetPath = new DatasetPath(dataset, targetPath);
    org.apache.hadoop.fs.Path fullPath = pathValidator.getFullPath(datasetPath);
    
    // HDFS_USERNAME is the next param to the bash script
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    
    String localDir = DigestUtils.sha256Hex(fullPath.toString());
    String stagingDir = settings.getStagingDir() + File.separator + localDir;
    
    File unzipDir = new File(stagingDir);
    unzipDir.mkdirs();
    
    List<String> commands = new ArrayList<>();
    commands.add(settings.getHopsworksDomainDir() + "/bin/unzip-background.sh");
    commands.add(stagingDir);
    commands.add(fullPath.toString());
    commands.add(hdfsUser);
    
    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
    String stdout = "", stderr = "";
    settings.addUnzippingState(fullPath.toString());
    try {
      int result = commandExecutor.executeCommand();
      stdout = commandExecutor.getStandardOutputFromCommand();
      stderr = commandExecutor.getStandardErrorFromCommand();
      if (result == 2) {
        throw new AppException(Response.Status.EXPECTATION_FAILED.
            getStatusCode(),
            "Not enough free space on the local scratch directory to download and unzip this file."
                + "Talk to your admin to increase disk space at the path: hopsworks/staging_dir");
      }
      if (result != 0) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(),
            "Could not unzip the file at path: " + fullPath);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "Interrupted exception. Could not unzip the file at path: " + fullPath);
    } catch (IOException ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "IOException. Could not unzip the file at path: " + fullPath);
    }
    
    return Response.noContent().build();
  }
  
  @Path("/{dsName}/blobs")
  public BlobsResource blobs(@PathParam("dsName") String dataSetName, @Context SecurityContext sc) throws AppException {
    Dataset ds = getDataset(dataSetName);
    this.blobsResource.setProject(project);
    this.blobsResource.setDataset(ds);
    return this.blobsResource;
  }
  
  private GenericEntity<InodeView> getFileHelper(Inode inode, String path){
    InodeView inodeView = new InodeView(inode, path+ "/" + inode.getInodePK().getName());
    inodeView.setUnzippingState(settings.getUnzippingState(
        path+ "/" + inode.getInodePK().getName()));
    Users user = userFacade.findByUsername(inodeView.getOwner());
    if (user != null) {
      inodeView.setOwner(user.getFname() + " " + user.getLname());
      inodeView.setEmail(user.getEmail());
    }
    
    return new GenericEntity<InodeView>(inodeView) { };
  }
  
  private GenericEntity<List<InodeView>> getDirHelper(Inode inode,
      String path, boolean isShared){
    List<Inode> cwdChildren = inodes.getChildren(inode);
    
    List<InodeView> kids = new ArrayList<>();
    for (Inode i : cwdChildren) {
      InodeView inodeView = new InodeView(i, path + "/" + i.getInodePK().getName());
      if (isShared) {
        //Get project of project__user the inode is owned by
        inodeView.setOwningProjectName(hdfsUsersBean.getProjectName(i.getHdfsUser().getName()));
      }
      inodeView.setUnzippingState(settings.getUnzippingState(path + "/" + i.getInodePK().getName()));
      Users user = userFacade.findByUsername(inodeView.getOwner());
      if (user != null) {
        inodeView.setOwner(user.getFname() + " " + user.getLname());
        inodeView.setEmail(user.getEmail());
      }
      kids.add(inodeView);
    }
    return new GenericEntity<List<InodeView>>(kids) { };
  }
}
