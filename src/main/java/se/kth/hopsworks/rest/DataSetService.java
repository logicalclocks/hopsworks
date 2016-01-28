package se.kth.hopsworks.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.ErasureCodeJob;
import se.kth.bbc.fileoperations.ErasureCodeJobConfiguration;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodeView;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.controller.DataSetDTO;
import se.kth.hopsworks.controller.DatasetController;
import se.kth.hopsworks.controller.FileTemplateDTO;
import se.kth.hopsworks.controller.JobController;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.dataset.DatasetRequest;
import se.kth.hopsworks.dataset.DatasetRequestFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.meta.db.TemplateFacade;
import se.kth.hopsworks.meta.entity.Template;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.util.Settings;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DataSetService {

  private final static Logger logger = Logger.getLogger(DataSetService.class.
          getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetRequestFacade datasetRequest;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private UserManager userBean;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FileOperations fileOps;
  @EJB
  private InodeFacade inodes;
  @Inject
  private UploadService uploader;
  @EJB
  private TemplateFacade template;
  @EJB
  private DatasetController datasetController;
  @EJB
  private AsynchronousJobExecutor async;
  @EJB
  private UserFacade userfacade;
  @EJB
  private JobController jobcontroller;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private Settings settings;
  @Inject
  private DownloadService downloader;

  
  private Integer projectId;
  private Project project;
  private String path;
  private Dataset dataset;

  public DataSetService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
    String projectPath = settings.getProjectPath(this.project.getName());
    this.path = projectPath + File.separator;
  }

  public Integer getProjectId() {
    return projectId;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findDataSetsInProjectID(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    Inode parent;
    InodeView inodeView;
    Users user;
    List<InodeView> kids = new ArrayList<>();

    String projPath = Settings.getProjectPath(this.project.getName());

    Collection<Dataset> dsInProject = this.project.getDatasetCollection();
    for (Dataset ds : dsInProject) {
      parent = inodes.findParent(ds.getInode());
      inodeView = new InodeView(parent, ds, projPath + "/" + ds.getInode()
          .getInodePK().getName());
      user = userfacade.findByUsername(inodeView.getOwner());
      if (user != null) {
        inodeView.setOwner(user.getFname() + " " + user.getLname());
        inodeView.setEmail(user.getEmail());
      }
      kids.add(inodeView);
    }

    GenericEntity<List<InodeView>> inodViews
            = new GenericEntity<List<InodeView>>(kids) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            inodViews).build();
  }

  /**
   * Get the inodes in the given project-relative path.
   * <p/>
   * @param path
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getDirContent(
          @PathParam("path") String path,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    String fullpath = getFullPath(path);
    List<Inode> cwdChildren;
    try {
      cwdChildren = inodes.getChildren(fullpath);
    } catch (IllegalArgumentException ex) {
      logger.log(Level.WARNING, "Trying to access children of file.", ex);
      throw new AppException(Response.Status.NO_CONTENT.getStatusCode(),
              "Cannot list the directory contents of a regular file.");
    } catch (FileNotFoundException ex) {
      logger.log(Level.WARNING, "Trying to access non-existent path.", ex);
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Path not found.");
    }
    List<InodeView> kids = new ArrayList<>();
    InodeView inodeView;
    Users user;
    for (Inode i : cwdChildren) {

      inodeView = new InodeView(i, fullpath + "/" + i.getInodePK().getName());
      user = userfacade.findByUsername(inodeView.getOwner());
      if (user != null) {
        inodeView.setOwner(user.getFname() + " " + user.getLname());
        inodeView.setEmail(user.getEmail());
      }
      kids.add(inodeView);
    }

    GenericEntity<List<InodeView>> inodViews
            = new GenericEntity<List<InodeView>>(kids) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            inodViews).build();
  }

  @POST
  @Path("/shareDataSet")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response shareDataSet(
          DataSetDTO dataSet,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {

    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    JsonResponse json = new JsonResponse();
    Inode parent = inodes.getProjectRoot(this.project.getName());
    if (dataSet == null || dataSet.getName() == null || dataSet.getName().
            isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }
    if (dataSet.getProjectId() == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "No project selected.");
    }
    Project proj = projectFacade.find(dataSet.getProjectId());
    if (proj == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    Inode inode = inodes.findByParentAndName(parent, dataSet.getName());
    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);
    if (ds == null) {//if parent id and project are not the same it is a shared ds.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You can not share this dataset you are not the owner.");
    }

    Dataset dst = datasetFacade.findByProjectAndInode(proj, inode);
    if (dst != null) {//proj already have the dataset.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Dataset already in " + proj.getName());
    }

    DatasetRequest dsReq = datasetRequest.findByProjectAndDataset(proj, ds);

    Dataset newDS = new Dataset(inode, proj);
    if (dataSet.getDescription() != null) {
      newDS.setDescription(dataSet.getDescription());
    }
    if (!dataSet.isEditable()) {
      newDS.setEditable(false);
    }
    // if the dataset is not requested or is requested by a data scientist
    // set status to pending. 
    if (dsReq == null || dsReq.getProjectTeam().getTeamRole().equals(
            AllowedRoles.DATA_SCIENTIST)) {
      newDS.setStatus(Dataset.PENDING);
    }
    datasetFacade.persistDataset(newDS);
    if (dsReq != null) {
      datasetRequest.remove(dsReq);//the dataset is shared so remove the request.
    }
    if (newDS.isEditable()) {
      try {
        FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
                FsAction.NONE, true);
        datasetController.changePermission(inodes.getPath(newDS.getInode()),
                user, project, fsPermission);
      } catch (AccessControlException ex) {
        throw new AccessControlException(
                "Permission denied: Can not change the permission of this file.");
      } catch (IOException e) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "Error while creating directory: " + e.
                getLocalizedMessage());
      }
    }
    activityFacade.persistActivity(ActivityFacade.SHARED_DATA + dataSet.
            getName() + " with project " + proj.getName(), project, user);

    json.setSuccessMessage("The Dataset was successfully shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/accept/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response acceptRequest(@PathParam("inodeId") Integer inodeId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (inodeId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);
    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }
    hdfsUsersBean.shareDataset(this.project, ds);
    ds.setStatus(Dataset.ACCEPTED);
    datasetFacade.merge(ds);
    json.setSuccessMessage("The Dataset is now accessable.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/reject/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response rejectRequest(@PathParam("inodeId") Integer inodeId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (inodeId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);

    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    datasetFacade.remove(ds);
    json.setSuccessMessage("The Dataset has been removed.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/createTopLevelDataSet")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createTopLevelDataSet(
          DataSetDTO dataSet,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());

    try {
      datasetController.createDataset(user, project, dataSet.getName(), dataSet.
              getDescription(), dataSet.getTemplate(), dataSet.isSearchable(),
              false);
    } catch (NullPointerException c) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), c.
              getLocalizedMessage());
    } catch (IllegalArgumentException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Failed to create dataset: " + e.getLocalizedMessage());
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Failed to create dataset: " + e.
              getLocalizedMessage());
    }

    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("The Dataset was created successfully.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  //TODO: put this in DatasetController.
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createDataSetDir(
          DataSetDTO dataSetName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {
    JsonResponse json = new JsonResponse();
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String newPath = getFullPath(dataSetName.getName());
    while (newPath.startsWith("/")) {
      newPath = newPath.substring(1);
    }
    String[] fullPathArray = newPath.split(File.separator);
    String[] datasetRelativePathArray = Arrays.copyOfRange(fullPathArray, 3,
            fullPathArray.length);
    String dsPath = File.separator + Settings.DIR_ROOT + File.separator
            + fullPathArray[1];
    //Check if the DataSet is writeable.
    if (!fullPathArray[1].equals(this.project.getName())) {
      if (!this.dataset.isEditable()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not create a folder inside a shared dataset.");
      }
    }
    StringBuilder dsRelativePath = new StringBuilder();
    for (String s : datasetRelativePathArray) {
      dsRelativePath.append(s).append("/");
    }

    try {
      datasetController.createSubDirectory(user, project, fullPathArray[2],
              dsRelativePath.toString(), dataSetName.getTemplate(), dataSetName.
              getDescription(), dataSetName.isSearchable());
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not create a folder in "
              + fullPathArray[2]);
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error while creating directory: " + e.
              getLocalizedMessage());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Invalid directory: " + e.getLocalizedMessage());
    }
    json.setSuccessMessage("A directory was created at " + dsPath);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @DELETE
  @Path("/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removedataSetdir(
          @PathParam("fileName") String fileName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {
    boolean success = false;
    JsonResponse json = new JsonResponse();
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    if (fileName == null || fileName.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }
    String filePath = getFullPath(fileName);
    String[] pathArray = filePath.split(File.separator);
    //if the path does not contain this project name it is shared.
    if (!pathArray[2].equals(this.project.getName())) { // /Projects/project/ds

      if (pathArray.length > 4 && !this.dataset.isEditable()) {// a folder in the dataset
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not perform this action on a shard dataset.");
      }
      if (!this.dataset.isEditable()) {
        //remove the entry in the table that represents shared ds
        //but leave the dataset in hdfs b/c the user does not have the right to delete it.
        hdfsUsersBean.unShareDataset(project, dataset);
        datasetFacade.removeDataset(this.dataset);
        json.setSuccessMessage(ResponseMessages.SHARED_DATASET_REMOVED);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(json).build();
      }
    }

    try {
      success = datasetController.deleteDataset(filePath, user, project);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not delete the file " + filePath);
    } catch (IOException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not delete the file at " + filePath);
    }
    if (!success) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not delete the file at " + filePath);
    }
    //remove the group associated with this dataset if the dataset is toplevel ds 
    if (filePath.endsWith(this.dataset.getInode().getInodePK().getName())) {
        hdfsUsersBean.deleteDatasetGroup(this.dataset);
    }
    json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("fileExists/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response checkFileExist(@PathParam("path") String path,
          @Context SecurityContext sc) throws
          AppException, AccessControlException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    if (path == null) {
      path = "";
    }
    path = getFullPath(path);

    try {
      boolean exists = this.fileOps.exists(path);

      //check if the path is a file only if it exists
      if (!exists || this.fileOps.isDir(path)) {
        throw new IOException("The file does not exist");
      }
      //tests if the user have permission to access this path
      dfs.getDfsOps(username).open(path);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not download the file ");
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + path);
    }
    Response.ResponseBuilder response = Response.ok();
    return response.build();
  }

  @GET
  @Path("isDir/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response isDir(@PathParam("path") String path) throws
          AppException {

    if (path == null) {
      path = "";
    }
    path = getFullPath(path);

    try {
      boolean exists = this.fileOps.exists(path);
      boolean isDir = this.fileOps.isDir(path);

      String message = "";
      JsonResponse response = new JsonResponse();

      //if it exists and it's not a dir, it must be a file
      if (exists && !isDir) {
        message = "FILE";
        response.setSuccessMessage(message);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(response).build();
      } else if (exists && isDir) {
        message = "DIR";
        response.setSuccessMessage(message);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(response).build();
      }

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + path);
    }
    throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            "The requested path does not resolve to a valid dir");
  }

  @GET
  @Path("countFileBlocks/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response countFileBlocks(@PathParam("path") String path) throws
          AppException {

    if (path == null) {
      path = "";
    }
    path = getFullPath(path);

    try {

      String blocks = this.fileOps.getFileBlocks(path);
      String response = blocks;

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + path);
    }
  }

  @Path("fileDownload/{path: .+}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public DownloadService downloadDS(@PathParam("path") String path,
          @Context SecurityContext sc) throws
          AppException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    if (path == null) {
      path = "";
    }
    path = getFullPath(path);
    String[] pathArray = path.split(File.separator);
    if (!pathArray[2].equals(this.project.getName())) {
      if (!this.dataset.isEditable()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not download a shared dataset.");
      }
    }
    if (!path.endsWith(File.separator)) {
      path = path + File.separator;
    }

    this.downloader.setPath(path);
    this.downloader.setUsername(username);
    return downloader;
  }

  @Path("compressFile/{path: .+}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response compressFile(@PathParam("path") String path,
          @Context SecurityContext context) throws
          AppException {

    if (path == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File not found");
    }

    path = this.getFullPath(path);
    String parts[] = path.split(File.separator);

    if (!parts[2].equals(this.project.getName())) {
      if (!this.dataset.isEditable()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not compress files in a shared dataset.");
      }
    }

    if (!path.endsWith(File.separator)) {
      path = path + File.separator;
    }

    Users user = this.userfacade.findByEmail(context.getUserPrincipal().
            getName());

    ErasureCodeJobConfiguration ecConfig
            = (ErasureCodeJobConfiguration) JobConfiguration.JobConfigurationFactory.
            getJobConfigurationTemplate(JobType.ERASURE_CODING);
    ecConfig.setFilePath(path);
    System.out.println("PREparing for erasure coding");

    //persist the job in the database
    JobDescription jobdesc = this.jobcontroller.createJob(user, project,
            ecConfig);
    System.out.println("job persisted in the database");
    //instantiate the job
    ErasureCodeJob encodeJob = new ErasureCodeJob(jobdesc, this.async, user,
            settings.getHadoopDir());
    //persist a job execution instance in the database and get its id
    Execution exec = encodeJob.requestExecutionId();
    System.out.println("\nSTarting the erasure coding job\n");
    if (exec != null) {
      //start the actual job execution i.e. compress the file in a different thread
      this.async.startExecution(encodeJob);
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Failed to persist JobHistory. File compression aborted");
    }

    String response = "File compression runs in background";
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(response);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @Path("upload/{path: .+}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public UploadService upload(
          @PathParam("path") String path, @Context SecurityContext sc,
          @QueryParam("templateId") int templateId) throws AppException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    if (path == null) {
      path = "";
    }
    path = getFullPath(path);
    String[] pathArray = path.split(File.separator);
    if (!pathArray[2].equals(this.project.getName())) {
      if (!this.dataset.isEditable()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not upload to a shared dataset.");
      }
    }
    if (!path.endsWith(File.separator)) {
      path = path + File.separator;
    }

    if (templateId != 0 && templateId != -1) {
      this.uploader.setTemplateId(templateId);
    }

    this.uploader.setPath(path);
    this.uploader.setUsername(username);

    return this.uploader;
  }

  @POST
  @Path("/attachTemplate")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response attachTemplate(FileTemplateDTO filetemplateData) throws
          AppException {

    if (filetemplateData == null || filetemplateData.getInodePath() == null
            || filetemplateData.getInodePath().equals("")) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.TEMPLATE_INODEID_EMPTY);
    }

    String inodePath = filetemplateData.getInodePath();
    int templateid = filetemplateData.getTemplateId();

    Inode inode = inodes.getInodeAtPath(inodePath);
    Template temp = template.findByTemplateId(templateid);
    temp.getInodes().add(inode);

    try {
      //persist the relationship
      this.template.updateTemplatesInodesMxN(temp);
    } catch (DatabaseException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              ResponseMessages.TEMPLATE_NOT_ATTACHED);
    }

    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("The template was attached to file "
            + inode.getId());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  private String getFullPath(String path) throws AppException {
    //Strip leading slashes.
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    String dsName;
    String projectName;
    String[] parts = path.split(File.separator);
    if (parts != null && parts[0].contains(Settings.SHARED_FILE_SEPARATOR)) {
      //we can split the string and get the project name, but we have to 
      //make sure that the user have access to the dataset.
      String[] shardDS = parts[0].split(Settings.SHARED_FILE_SEPARATOR);
      if (shardDS.length < 2) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.DATASET_NOT_FOUND);
      }
      projectName = shardDS[0];
      dsName = shardDS[1];
      Inode parent = inodes.getProjectRoot(projectName);
      Inode dsInode = inodes.findByParentAndName(parent, dsName);
      this.dataset = datasetFacade.findByProjectAndInode(this.project, dsInode);
      if (this.dataset == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.DATASET_NOT_FOUND);
      }
      if (this.dataset.getStatus() == Dataset.PENDING) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Dataset is not yet accessible. Accept the share requst to access it.");
      }
      path = path.replaceFirst(projectName + Settings.SHARED_FILE_SEPARATOR
              + dsName, projectName
              + File.separator + dsName);
    } else if (parts != null) {
      dsName = parts[0];
      Inode parent = inodes.getProjectRoot(this.project.getName());
      Inode dsInode = inodes.findByParentAndName(parent, dsName);
      this.dataset = datasetFacade.findByProjectAndInode(this.project, dsInode);
      if (this.dataset == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.DATASET_NOT_FOUND);
      }
      return this.path + path;
    }
    return File.separator + Settings.DIR_ROOT + File.separator
            + path;
  }

}
