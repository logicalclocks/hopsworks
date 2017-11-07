package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.project.util.DsDTOValidator;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.api.util.DownloadService;
import io.hops.hopsworks.api.util.FilePreviewImageTypes;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.api.util.UploadService;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.DataSetDTO;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetRequest;
import io.hops.hopsworks.common.dao.dataset.DatasetRequestFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.dao.metadata.db.TemplateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.MoveDTO;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.erasureCode.ErasureCodeJob;
import io.hops.hopsworks.common.jobs.erasureCode.ErasureCodeJobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.SystemCommandExecutor;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import javax.ws.rs.Consumes;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;

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
  private UserFacade userFacade;
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
  @EJB
  private YarnJobsMonitor jobsMonitor;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private DsDTOValidator dtoValidator;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  private Integer projectId;
  private Project project;

  public DataSetService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
    String projectPath = Settings.getProjectPath(this.project.getName());
  }

  public Integer getProjectId() {
    return projectId;
  }

  @GET
  @Path("unzip/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response unzip(@PathParam("path") String path,
          @Context SecurityContext sc) throws
          AppException, AccessControlException {

    Response.Status resp = Response.Status.OK;
    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();


    // HDFS_USERNAME is the next param to the bash script
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);

    String localDir = DigestUtils.sha256Hex(fullPath);
    String stagingDir = settings.getStagingDir() + File.separator + localDir;

    File unzipDir = new File(stagingDir);
    unzipDir.mkdirs();

//    Set<PosixFilePermission> perms = new HashSet<>();
//    //add owners permission
//    perms.add(PosixFilePermission.OWNER_READ);
//    perms.add(PosixFilePermission.OWNER_WRITE);
//    perms.add(PosixFilePermission.OWNER_EXECUTE);
//    //add group permissions
//    perms.add(PosixFilePermission.GROUP_READ);
//    perms.add(PosixFilePermission.GROUP_WRITE);
//    perms.add(PosixFilePermission.GROUP_EXECUTE);
//    //add others permissions
//    perms.add(PosixFilePermission.OTHERS_READ);
//    perms.add(PosixFilePermission.OTHERS_WRITE);
//    perms.add(PosixFilePermission.OTHERS_EXECUTE);
//    Files.setPosixFilePermissions(Paths.get(unzipDir), perms);
    List<String> commands = new ArrayList<>();
//    commands.add("/bin/bash");
//    commands.add("-c");
    commands.add(settings.getHopsworksDomainDir() + "/bin/unzip-background.sh");
    commands.add(stagingDir);
    commands.add(fullPath);
    commands.add(hdfsUser);

    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
    String stdout = "", stderr = "";
    settings.addUnzippingState(fullPath);
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

    return noCacheResponse.getNoCacheResponseBuilder(resp).build();
  }

  @GET
  @Path("/getContent/")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findDataSetsInProjectID(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    List<InodeView> kids = new ArrayList<>();
    Collection<Dataset> dsInProject = this.project.getDatasetCollection();
    for (Dataset ds : dsInProject) {
      String path = datasetController.getDatasetPath(ds).toString();
      List<Dataset> inodeOccurrence = datasetFacade.findByInodeId(ds.getInodeId());
      int sharedWith = inodeOccurrence.size() - 1; // -1 for ds itself
      InodeView inodeView = new InodeView(inodes.findParent(ds.getInode()), ds, path);
      Users user = userFacade.findByUsername(inodeView.getOwner());
      if (user != null) {
        inodeView.setOwner(user.getFname() + " " + user.getLname());
        inodeView.setEmail(user.getEmail());
      }
      inodeView.setSharedWith(sharedWith);
      kids.add(inodeView);
    }

    GenericEntity<List<InodeView>> inodViews
            = new GenericEntity<List<InodeView>>(kids) { };
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
  @Path("/getContent/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getDirContent(
          @PathParam("path") String path,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();
    Inode parent = dsPath.validatePathExists(inodes,true);
    List<Inode> cwdChildren = inodes.getChildren(parent);

    List<InodeView> kids = new ArrayList<>();
    for (Inode i : cwdChildren) {
      InodeView inodeView = new InodeView(i, fullPath + "/" + i.getInodePK().getName());
      if (dsPath.getDs().isShared()) {
        //Get project of project__user the inode is owned by
        inodeView.setOwningProjectName(hdfsUsersBean.getProjectName(i.getHdfsUser().getName()));
      }
      inodeView.setUnzippingState(settings.getUnzippingState(
              fullPath + "/" + i.getInodePK().getName()));
      Users user = userFacade.findByUsername(inodeView.getOwner());
      if (user != null) {
        inodeView.setOwner(user.getFname() + " " + user.getLname());
        inodeView.setEmail(user.getEmail());
      }
      kids.add(inodeView);
    }
    GenericEntity<List<InodeView>> inodeViews
            = new GenericEntity<List<InodeView>>(kids) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            inodeViews).build();
  }

  @GET
  @Path("/getFile/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getFile(@PathParam("path") String path,
          @Context SecurityContext sc) throws
          AppException, AccessControlException {

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    // The inode can be both a file and a directory
    String fullPath = dsPath.getFullPath().toString();
    Inode inode = dsPath.validatePathExists(inodes,null);

    InodeView inodeView = new InodeView(inode, fullPath+ "/" + inode.getInodePK().
            getName());
    inodeView.setUnzippingState(settings.getUnzippingState(
            fullPath+ "/" + inode.getInodePK().getName()));
    Users user = userFacade.findByUsername(inodeView.getOwner());
    if (user != null) {
      inodeView.setOwner(user.getFname() + " " + user.getLname());
      inodeView.setEmail(user.getEmail());
    }

    GenericEntity<InodeView> inodeViews
            = new GenericEntity<InodeView>(inodeView) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            inodeViews).build();
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
    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, false);
    JsonResponse json = new JsonResponse();

    // Check target project
    Project proj = projectFacade.find(dataSet.getProjectId());
    if (proj == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }

    Dataset dst = datasetFacade.findByProjectAndInode(proj, ds.getInode());
    if (dst != null) {//proj already have the dataset.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Dataset already in " + proj.getName());
    }

    // Create the new Dataset entry
    Dataset newDS = new Dataset(ds, proj);
    newDS.setShared(true);

    // if the dataset is not requested or is requested by a data scientist
    // set status to pending.
    DatasetRequest dsReq = datasetRequest.findByProjectAndDataset(proj, ds);
    if (dsReq == null || dsReq.getProjectTeam().getTeamRole().equals(
            AllowedRoles.DATA_SCIENTIST)) {
      newDS.setStatus(Dataset.PENDING);
    } else {
      hdfsUsersBean.shareDataset(proj, ds);
    }

    datasetFacade.persistDataset(newDS);

    if (dsReq != null) {
      datasetRequest.remove(dsReq);//the dataset is shared so remove the request.
    }

    activityFacade.persistActivity(ActivityFacade.SHARED_DATA + dataSet.
            getName() + " with project " + proj.getName(), project, user);

    json.setSuccessMessage("The Dataset was successfully shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/unshareDataSet")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response unshareDataSet(
          DataSetDTO dataSet,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {

    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    JsonResponse json = new JsonResponse();

    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, true);

    for (int projectId : dataSet.getProjectIds()) {
      Project proj = projectFacade.find(projectId);
      if (proj == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.PROJECT_NOT_FOUND);
      }

      Dataset dst = datasetFacade.findByProjectAndInode(proj, ds.getInode());
      if (dst == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Dataset not shared with " + proj.getName());
      }

      hdfsUsersBean.unshareDataset(proj, ds);
      datasetFacade.removeDataset(dst);
      activityFacade.persistActivity(ActivityFacade.UNSHARED_DATA + dataSet.
              getName() + " with project " + proj.getName(), project, user);
    }
    json.setSuccessMessage("The Dataset was successfully unshared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/projectsSharedWith")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response getProjectSharedWith(
          DataSetDTO dataSet,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {

    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, true);

    List<Project> list = datasetFacade.findProjectSharedWith(project, ds.getInode());
    GenericEntity<List<Project>> projects = new GenericEntity<List<Project>>(
            list) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projects).build();
  }

  @POST
  @Path("/makeEditable")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response makeEditable(
          DataSetDTO dataSet,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {

    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, false);

    JsonResponse json = new JsonResponse();

    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      // Change permission as super user
      FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
              FsAction.NONE, true);
      datasetController.recChangeOwnershipAndPermission(
          datasetController.getDatasetPath(ds),
          fsPermission, null, null, null, dfso);
      datasetController.changeEditable(ds, true);
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

    json.setSuccessMessage("The Dataset was successfully made editable.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/removeEditable")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeEditable(
          DataSetDTO dataSet,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {

    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, false);
    JsonResponse json = new JsonResponse();

    DistributedFileSystemOps dfso = null;
    try {
      // change the permissions as superuser
      dfso = dfs.getDfsOps();
      FsPermission fsPermission = new FsPermission(FsAction.ALL,
              FsAction.READ_EXECUTE,
              FsAction.NONE, false);
      datasetController.recChangeOwnershipAndPermission(
          datasetController.getDatasetPath(ds),
          fsPermission, null, null, null, dfso);
      datasetController.changeEditable(ds, false);
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

    json.setSuccessMessage("The Dataset was successfully made not editable.");
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
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    if (username == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "User not found");
    }
    DistributedFileSystemOps udfso = dfs.getDfsOps(username);

    try {
      datasetController.createDataset(user, project, dataSet.getName(),
          dataSet.getDescription(), dataSet.getTemplate(), dataSet.isSearchable(),
          false, dfso);
      //Generate README.md for the dataset if the user requested it
      if (dataSet.isGenerateReadme()) {
        //Persist README.md to hdfs
        datasetController.generateReadme(udfso, dataSet.getName(), dataSet.getDescription(),
            project.getName());
      }
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Failed to create dataset: " + e.
              getLocalizedMessage());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }

    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("The Dataset was created successfully.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response createDataSetDir(
          DataSetDTO dataSetName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {

    JsonResponse json = new JsonResponse();
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());

    DsPath dsPath = pathValidator.validatePath(this.project, dataSetName.getName());
    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    Dataset ds = dsPath.getDs();
    if (!ds.isEditable()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_EDITABLE);
    }
    String dsName = ds.getInode().getInodePK().getName();

    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      dfso = dfs.getDfsOps();
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      if (username != null) {
        udfso = dfs.getDfsOps(username);
      }
      datasetController.createSubDirectory(this.project, fullPath,
          dataSetName.getTemplate(), dataSetName.getDescription(),
          dataSetName.isSearchable(), udfso);

    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not create a folder in "
              + dsName);
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error while creating directory: " + e.
              getLocalizedMessage());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    json.setSuccessMessage("A directory was created at " + fullPath);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  /**
   * This function is used only for deletion of dataset directories
   * as it does not accept a path
   * @param fileName
   * @param sc
   * @param req
   * @return 
   * @throws io.hops.hopsworks.common.exception.AppException 
   * @throws org.apache.hadoop.security.AccessControlException
   */
  @DELETE
  @Path("/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response removedataSetdir(
          @PathParam("fileName") String fileName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {
    boolean success = false;
    JsonResponse json = new JsonResponse();

    DsPath dsPath = pathValidator.validatePath(this.project, fileName);
    Dataset ds = dsPath.getDs();
    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();

    if (ds.isShared()) {
      // The user is trying to delete a dataset. Drop it from the table
      // But leave it in hopsfs because the user doesn't have the right to delete it
      hdfsUsersBean.unShareDataset(project, ds);
      datasetFacade.removeDataset(ds);
      json.setSuccessMessage(ResponseMessages.SHARED_DATASET_REMOVED);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json).build();
    }

    DistributedFileSystemOps dfso = null;
    try {
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = datasetController.getOwningProject(ds);
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && projectTeamFacade.findCurrentRole(owning, user).equals(AllowedRoles.DATA_OWNER) && owning.equals(
          project)) {
        dfso = dfs.getDfsOps();// do it as super user
      } else {
        dfso = dfs.getDfsOps(username);// do it as project user
      }
      success = datasetController.
              deleteDatasetDir(ds, fullPath, dfso);
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
      hdfsUsersBean.deleteDatasetGroup(ds);
    } catch (IOException ex) {
      //FIXME: take an action?
      logger.log(Level.WARNING,
              "Error while trying to delete a dataset group", ex);
    }
    json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }
  
  /**
   * Removes corrupted files from incomplete downloads.
   * 
   * @param fileName
   * @param req
   * @param sc
   * @return 
   * @throws io.hops.hopsworks.common.exception.AppException
   * @throws org.apache.hadoop.security.AccessControlException
   */
  @DELETE
  @Path("corrupted/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response removeCorrupted(
      @PathParam("fileName") String fileName,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException,
      AccessControlException {
    JsonResponse json = new JsonResponse();
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());

    DsPath dsPath = pathValidator.validatePath(this.project, fileName);
    Dataset ds = dsPath.getDs();

    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    org.apache.hadoop.fs.Path dsRelativePath = dsPath.getDsRelativePath();

    if (dsRelativePath.depth() == 0) {
      logger.log(Level.SEVERE,
          "Use DELETE /{datasetName} to delete top level dataset.");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.INTERNAL_SERVER_ERROR);
    }

    DistributedFileSystemOps dfso = null;
    try {
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = datasetController.getOwningProject(ds);
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && owning.equals(project)) {
        dfso = dfs.getDfsOps();// do it as super user
        FileStatus fs = dfso.getFileStatus(fullPath);
        String owner = fs.getOwner();
        long len = fs.getLen();
        if (owner.equals(settings.getHopsworksUser()) && len == 0) {
          dfso.rm(fullPath, true);
          json.setSuccessMessage(ResponseMessages.FILE_CORRUPTED_REMOVED_FROM_HDFS);
          return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
              json).build();
        }
      }
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

    throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        "Could not delete the file at " + fullPath);

  }

  /**
   * Differently from the previous function, this accepts a path.
   * If it is used to delete a dataset directory it will throw an exception
   * (line 779)
   * @param fileName
   * @param req
   * @param sc
   * @return 
   * @throws io.hops.hopsworks.common.exception.AppException
   * @throws org.apache.hadoop.security.AccessControlException
   */
  @DELETE
  @Path("file/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response removefile(
          @PathParam("fileName") String fileName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {
    boolean success = false;
    JsonResponse json = new JsonResponse();
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());

    DsPath dsPath = pathValidator.validatePath(this.project, fileName);
    Dataset ds = dsPath.getDs();
    
    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    org.apache.hadoop.fs.Path dsRelativePath = dsPath.getDsRelativePath();

    if (dsRelativePath.depth() == 0) {
      logger.log(Level.SEVERE,
          "Use DELETE /{datasetName} to delete top level dataset.");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.INTERNAL_SERVER_ERROR);
    }

    DistributedFileSystemOps dfso = null;
    try {
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
      //Find project of dataset as it might be shared
      Project owning = datasetController.getOwningProject(ds);
      boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
      if (isMember && projectTeamFacade.findCurrentRole(owning, user).equals(AllowedRoles.DATA_OWNER) && owning.equals(
          project)) {
        dfso = dfs.getDfsOps();// do it as super user
      } else {
        dfso = dfs.getDfsOps(username);// do it as project user
      }
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
    json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  /**
   * Move and Rename operations handled here
   *
   * @param req
   * @param dto
   * @param sc
   * @return
   * @throws AppException
   * @throws AccessControlException
   */
  @POST
  @Path("move")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response moveFile(
          @Context SecurityContext sc, @Context HttpServletRequest req,
          MoveDTO dto) throws
          AppException, AccessControlException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String username = hdfsUsersBean.getHdfsUserName(project, user);

    Inode sourceInode = inodes.findById(dto.getInodeId());
    if (sourceInode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Cannot find file/folder you are trying to move. Has it been deleted?");
    }

    String sourcePathStr = inodes.getPath(sourceInode);
    DsPath sourceDsPath = pathValidator.validatePath(this.project, sourcePathStr);
    DsPath destDsPath = pathValidator.validatePath(this.project, dto.getDestPath());

    Dataset sourceDataset = sourceDsPath.getDs();

    // The destination dataset project is already the correct one, as the path is given
    // (and parsed)
    Dataset destDataset = destDsPath.getDs();

    if (!datasetController.getOwningProject(sourceDataset).equals(
        destDataset.getProject())) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Cannot copy file/folder from another project.");
    }

    if (destDataset.isPublicDs()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
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
      if (isMember && projectTeamFacade.findCurrentRole(owning, user).equals(AllowedRoles.DATA_OWNER) && owning.equals(
          project)) {
        udfso = dfs.getDfsOps();// do it as super user
      } else {
        udfso = dfs.getDfsOps(username);// do it as project user
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

      JsonResponse response = new JsonResponse();
      response.setSuccessMessage("Moved");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Move at path:" + destPath.toString()
              + " failed. It is not a directory or you do not have permission to"
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

  /**
   * Copy operations handled here.
   *
   * @param req
   * @param dto
   * @param sc
   * @return
   * @throws AppException
   * @throws AccessControlException
   */
  @POST
  @Path("copy")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response copyFile(
          @Context SecurityContext sc, @Context HttpServletRequest req,
          MoveDTO dto) throws
          AppException, AccessControlException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String username = hdfsUsersBean.getHdfsUserName(project, user);

    Inode sourceInode = inodes.findById(dto.getInodeId());
    if (sourceInode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Cannot find file/folder you are trying to copy. Has it been deleted?");
    }
    String sourcePathStr = inodes.getPath(sourceInode);

    DsPath sourceDsPath = pathValidator.validatePath(this.project, sourcePathStr);
    DsPath destDsPath = pathValidator.validatePath(this.project, dto.getDestPath());

    Dataset sourceDataset = sourceDsPath.getDs();
    // The destination dataset project is already the correct one, as the
    // full path is given in the MoveDTO object
    Dataset destDataset = destDsPath.getDs();

    if (!datasetController.getOwningProject(sourceDataset).equals(
        destDataset.getProject())) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Cannot copy file/folder from another project.");
    }

    if (destDataset.isPublicDs()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Can not copy to a public dataset.");
    }

    org.apache.hadoop.fs.Path sourcePath = sourceDsPath.getFullPath();
    org.apache.hadoop.fs.Path destPath = destDsPath.getFullPath();

    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(username);

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

      JsonResponse response = new JsonResponse();
      response.setSuccessMessage("Copied");

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();

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

  @GET
  @Path("fileExists/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response checkFileExists(@PathParam("path") String path,
          @Context SecurityContext sc) throws
          AppException, AccessControlException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String username = hdfsUsersBean.getHdfsUserName(project, user);

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    dsPath.validatePathExists(inodes, false);
    org.apache.hadoop.fs.Path filePath = dsPath.getFullPath();

    DistributedFileSystemOps udfso = null;
    FSDataInputStream is = null;
    try {
      udfso = dfs.getDfsOps(username);

      //tests if the user have permission to access this path
      is = udfso.open(filePath);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not download the file ");
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " +filePath.toString());
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Error while closing stream.", ex);
        }
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    Response.ResponseBuilder response = Response.ok();
    return response.build();
  }

  @GET
  @Path("checkFileForDownload/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response checkFileForDownload(@PathParam("path") String path,
          @Context SecurityContext sc) throws
          AppException, AccessControlException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    DsPath dsPath = pathValidator.validatePath(this.project, path);
    Project owningProject = datasetController.getOwningProject(dsPath.getDs());
    //User must be accessing a dataset directly, not by being shared with another project.
    //For example, DS1 of project1 is shared with project2. User must be a member of project1 to download files
    if (owningProject.equals(project) && datasetController.isDownloadAllowed(project, user, dsPath.getFullPath().
        toString())) {
      return checkFileExists(path, sc);
    }
    JsonResponse response = new JsonResponse();
    response.setErrorMsg(ResponseMessages.DOWNLOAD_PERMISSION_ERROR);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.FORBIDDEN).entity(response).build();
  }

  @GET
  @Path("filePreview/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response filePreview(@PathParam("path") String path,
          @QueryParam("mode") String mode,
          @Context SecurityContext sc) throws
          AppException, AccessControlException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    String username = hdfsUsersBean.getHdfsUserName(project, user);

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    dsPath.validatePathExists(inodes,false);
    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    String fileName = fullPath.getName();

    DistributedFileSystemOps udfso = null;
    FSDataInputStream is = null;

    JsonResponse json = new JsonResponse();
    try {
      udfso = dfs.getDfsOps(username);

      //tests if the user have permission to access this path
      is = udfso.open(fullPath);

      //Get file type first. If it is not a known image type, display its 
      //binary contents instead
      String fileExtension = "txt"; // default file  type
      //Check if file contains a valid extension
      if (fileName.contains(".")) {
        fileExtension = fileName.substring(fileName.lastIndexOf(".")).replace(".", "").
                toUpperCase();
      }
      long fileSize = udfso.getFileStatus(fullPath).getLen();

      FilePreviewDTO filePreviewDTO = null;
      if (HopsUtils.isInEnum(fileExtension, FilePreviewImageTypes.class)) {
        //If it is an image smaller than 10MB download it otherwise thrown an error
        if (fileSize < settings.getFilePreviewImageSize()) {
          //Read the image in bytes and convert it to base64 so that is 
          //rendered properly in the front-end
          byte[] imageInBytes = new byte[(int)fileSize];
          is.readFully(imageInBytes);
          String base64Image = new Base64().encodeAsString(imageInBytes);
          filePreviewDTO = new FilePreviewDTO("image",
                  fileExtension.toLowerCase(), base64Image);
        } else {
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  "Image at " + fullPath.toString() + " is too big to display, " +
                  "please download it by double-clicking it instead");
        }
      } else {
        DataInputStream dis = new DataInputStream(is);
        try {
          int sizeThreshold = Settings.FILE_PREVIEW_TXT_SIZE_BYTES; //in bytes
          if (fileSize > sizeThreshold && !fileName.endsWith("README.md")
                && mode.equals("tail")) {
            dis.skipBytes((int) (fileSize - sizeThreshold));
          } else if (fileName.endsWith("README.md") &&
              fileSize > Settings.FILE_PREVIEW_TXT_SIZE_BYTES_README) {

            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "README.md must be smaller than "
                    + Settings.FILE_PREVIEW_TXT_SIZE_BYTES_README
                    + " to be previewd");
          } else  {
            sizeThreshold = (int)fileSize;
          }

          byte[] headContent = new byte[sizeThreshold];
          dis.readFully(headContent, 0, sizeThreshold);
          //File content
          filePreviewDTO = new FilePreviewDTO("text", fileExtension.
                  toLowerCase(), new String(headContent));

        } finally {
          dis.close();
        }
      }

      json.setData(filePreviewDTO);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not view the file ");
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + fullPath.toString());
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Error while closing stream.", ex);
        }
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("isDir/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response isDir(@PathParam("path") String path) throws
          AppException {

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    Inode inode = dsPath.validatePathExists(inodes, null);

    JsonResponse response = new JsonResponse();
    if (inode.isDir()) {
      response.setSuccessMessage("DIR");
    } else {
      response.setSuccessMessage("FILE");
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(response).build();
  }

  @GET
  @Path("countFileBlocks/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response countFileBlocks(@PathParam("path") String path) throws
          AppException {

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();

    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      String blocks = dfso.getFileBlocks(fullPath);

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(blocks).build();

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + fullPath);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  @Path("fileDownload")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public DownloadService downloadDS(@Context SecurityContext sc) throws
      AppException {
    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    this.downloader.setProject(project);
    this.downloader.setProjectUsername(hdfsUsersBean.getHdfsUserName(project, user));
    this.downloader.setUser(user);
    return downloader;
  }
  
  @Path("compressFile/{path: .+}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response compressFile(@PathParam("path") String path,
          @Context SecurityContext context) throws
          AppException {
    Users user = userBean.getUserByEmail(context.getUserPrincipal().getName());

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    Dataset ds = dsPath.getDs();
    if (ds.isShared() && !ds.isEditable() && !ds.isPublicDs()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.COMPRESS_ERROR);
    }

    ErasureCodeJobConfiguration ecConfig
            = (ErasureCodeJobConfiguration) JobConfiguration.JobConfigurationFactory.
            getJobConfigurationTemplate(JobType.ERASURE_CODING);
    ecConfig.setFilePath(fullPath.toString());

    //persist the job in the database
    Jobs jobdesc = this.jobcontroller.createJob(user, project,
            ecConfig);
    //instantiate the job
    ErasureCodeJob encodeJob = new ErasureCodeJob(jobdesc, this.async, user,
            settings.getHadoopSymbolicLinkDir(), jobsMonitor);
    //persist a job execution instance in the database and get its id
    Execution exec = encodeJob.requestExecutionId();
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

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    if (!dsPath.getDs().isEditable()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_EDITABLE);
    }
    
    Project owning = datasetController.getOwningProject(dsPath.getDs());
    //Is user a member of this project? If so get their role
    boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
    String role = null;
    if(isMember){
      role = projectTeamFacade.findCurrentRole(owning, user);
    }
     
    this.uploader.confFileUpload(dsPath, username, templateId, role);
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
}