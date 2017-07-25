package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.io.DataInputStream;
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
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import io.hops.hopsworks.api.filter.AllowedRoles;
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
import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.dao.metadata.db.TemplateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
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
import org.apache.commons.codec.digest.DigestUtils;

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
  private JupyterFacade jupyterFacade;

  private Integer projectId;
  private Project project;
  private String path;
  private Dataset dataset;

  public DataSetService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
    String projectPath = Settings.getProjectPath(this.project.getName());
    this.path = projectPath + File.separator;
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
    if (path == null) {
      path = "";
    }
    path = getFullPath(path);

    // HDFS_USERNAME is the next param to the bash script
    String email = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(email);
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);

    String localDir = DigestUtils.sha256Hex(path);
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
    commands.add(path);
    commands.add(hdfsUser);

    SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
    String stdout = "", stderr = "";
    settings.addUnzippingState(path);
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
                "Could not unzip the file at path: " + path);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Interrupted exception. Could not unzip the file at path: " + path);
    } catch (IOException ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "IOException. Could not unzip the file at path: " + path);
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

    Inode parent;
    InodeView inodeView;
    Users user;
    List<InodeView> kids = new ArrayList<>();
    boolean notebookDirExists = false;
    Collection<Dataset> dsInProject = this.project.getDatasetCollection();
    for (Dataset ds : dsInProject) {
      parent = inodes.findParent(ds.getInode());
      //If it is a shared dataset, put owner project in path
      String projPath = "";
      if (ds.isShared()) {
        projPath = Settings.getProjectPath(parent.getInodePK().getName());
      } else {
        projPath = Settings.getProjectPath(this.project.getName());
      }
      List<Dataset> inodeOccurrence = datasetFacade.
              findByInodeId(ds.getInodeId());
      int sharedWith = inodeOccurrence.size() - 1; // -1 for ds itself 
      inodeView = new InodeView(parent, ds, projPath + File.separator + ds.
              getInode()
              .getInodePK().getName());
      user = userFacade.findByUsername(inodeView.getOwner());
      if (user != null) {
        inodeView.setOwner(user.getFname() + " " + user.getLname());
        inodeView.setEmail(user.getEmail());
      }
      inodeView.setSharedWith(sharedWith);
      kids.add(inodeView);
      if (inodeView.getName().
              equals(Settings.DefaultDataset.ZEPPELIN.getName())) {
        notebookDirExists = true;
      }
    }
    //if there is a notebook datasets in project dir but not in Dataset table
    if (!notebookDirExists) {
      String projPath = Settings.getProjectPath(this.project.getName());

      Inode projectInode = inodes.getInodeAtPath(projPath);
      Inode ds = inodes.findByInodePK(projectInode,
              Settings.DefaultDataset.ZEPPELIN.getName(),
              HopsUtils.dataSetPartitionId(projectInode,
                      Settings.DefaultDataset.ZEPPELIN.getName()));
      if (ds != null) {
        logger.log(Level.INFO, "Notebook dir not in datasets, adding.");
        Dataset newDS = new Dataset(ds, this.project);
        newDS.setSearchable(false);
        newDS.setDescription(Settings.DefaultDataset.ZEPPELIN.getDescription());
        datasetFacade.persistDataset(newDS);

        inodeView = new InodeView(projectInode, newDS, projPath + File.separator
                + ds.getInodePK().getName());
        user = userFacade.findByUsername(inodeView.getOwner());
        if (user != null) {
          inodeView.setOwner(user.getFname() + " " + user.getLname());
          inodeView.setEmail(user.getEmail());
        }
        kids.add(inodeView);
      }
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
    String fullpath = getFullPath(path);
    List<Inode> cwdChildren;
    try {
      cwdChildren = inodes.getChildren(fullpath);
    } catch (FileNotFoundException ex) {
      logger.log(Level.WARNING, ex.getMessage(), ex);
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ex.getMessage());
    }
    List<InodeView> kids = new ArrayList<>();
    InodeView inodeView;
    Users user;
    for (Inode i : cwdChildren) {
      inodeView = new InodeView(i, fullpath + "/" + i.getInodePK().getName());
      inodeView.setUnzippingState(settings.getUnzippingState(
              fullpath + "/" + i.getInodePK().getName()));
      user = userFacade.findByUsername(inodeView.getOwner());
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
    String fullpath = getFullPath(path);
    Inode inode = inodes.getInodeAtPath(fullpath);

    if (inode == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    InodeView inodeView;
    Users user;

    inodeView = new InodeView(inode, fullpath + "/" + inode.getInodePK().
            getName());
    inodeView.setUnzippingState(settings.getUnzippingState(
            fullpath + "/" + inode.getInodePK().getName()));
    user = userFacade.findByUsername(inodeView.getOwner());
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
    Inode inode = inodes.findByInodePK(parent, dataSet.getName(),
            HopsUtils.dataSetPartitionId(parent, dataSet.getName()));
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
    newDS.setShared(true);

    if (dataSet.getDescription() != null) {
      newDS.setDescription(dataSet.getDescription());
    }
    if (!dataSet.isEditable()) {
      newDS.setEditable(false);
    }
    if (dataSet.isIsPublic()) {
      newDS.setPublicDs(true);
    }

    // if the dataset is not requested or is requested by a data scientist
    // set status to pending. 
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
          DataSetDTO dataSets,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {

    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    JsonResponse json = new JsonResponse();
    Inode parent = inodes.getProjectRoot(this.project.getName());
    if (dataSets == null || dataSets.getName() == null || dataSets.getName().
            isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }
    if (dataSets.getProjectIds() == null || dataSets.getProjectIds().isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "No project selected.");
    }
    for (int projectId : dataSets.getProjectIds()) {
      Project proj = projectFacade.find(projectId);
      if (proj == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.PROJECT_NOT_FOUND);
      }
      Inode inode = inodes.findByInodePK(parent, dataSets.getName(),
              HopsUtils.dataSetPartitionId(parent, dataSets.getName()));
      Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);
      if (ds == null) {//if parent id and project are not the same it is a shared ds.
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not unshare this dataset you are not the owner.");
      }

      Dataset dst = datasetFacade.findByProjectAndInode(proj, inode);
      if (dst == null) {//proj already have the dataset.
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Dataset not shared with " + proj.getName());
      }

      hdfsUsersBean.unshareDataset(proj, ds);

      datasetFacade.removeDataset(dst);

      activityFacade.persistActivity(ActivityFacade.UNSHARED_DATA + dataSets.
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

    List<Project> list = datasetFacade.findProjectSharedWith(project, dataSet.
            getName());
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

    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    JsonResponse json = new JsonResponse();
    Inode parent = inodes.getProjectRoot(this.project.getName());
    if (dataSet == null || dataSet.getName() == null || dataSet.getName().
            isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }

    Inode inode = inodes.findByInodePK(parent, dataSet.getName(),
            HopsUtils.dataSetPartitionId(parent, dataSet.getName()));
    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);
    if (ds == null) {//if parent id and project are not the same it is a shared ds.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You can not make this dataset editable you are not the owner.");
    }

    DistributedFileSystemOps udfso = null;
    try {
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
              FsAction.NONE, true);
      datasetController.changePermission(inodes.getPath(inode),
              user, project, fsPermission, udfso);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: Can not change the permission of this file.");
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error while creating directory: " + e.
              getLocalizedMessage());
    } finally {
      if (udfso != null) {
        udfso.close();
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

    Users user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    JsonResponse json = new JsonResponse();
    Inode parent = inodes.getProjectRoot(this.project.getName());
    if (dataSet == null || dataSet.getName() == null || dataSet.getName().
            isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }

    Inode inode = inodes.findByInodePK(parent, dataSet.getName(),
            HopsUtils.dataSetPartitionId(parent, dataSet.getName()));
    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);
    if (ds == null) {//if parent id and project are not the same it is a shared ds.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You can not make this dataset editable you are not the owner.");
    }

    DistributedFileSystemOps udfso = null;
    try {
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      FsPermission fsPermission = new FsPermission(FsAction.ALL,
              FsAction.READ_EXECUTE,
              FsAction.NONE, true);
      datasetController.recursiveChangeOwnershipAndPermission(inodes.getPath(
              inode),
              user, fsPermission, udfso);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: Can not change the permission of this file.");
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error while creating directory: " + e.
              getLocalizedMessage());
    } finally {
      if (udfso != null) {
        udfso.close();
      }
    }

    json.setSuccessMessage("The Dataset was successfully made editable.");
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
    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      dfso = dfs.getDfsOps();
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      if (username != null) {
        udfso = dfs.getDfsOps(username);
      }
      datasetController.createAndLogDataset(user, project, dataSet.getName(),
              dataSet.
              getDescription(), dataSet.getTemplate(), dataSet.isSearchable(),
              false, dfso, dfso); // both are dfso to create it as root user
      //Generate README.md for the dataset if the user requested it
      if (dataSet.isGenerateReadme()) {
        //Persist README.md to hdfs
        if (udfso != null) {
          String readmeFile = String.format(Settings.README_TEMPLATE, dataSet.
                  getName(), dataSet.getDescription());
          String readMeFilePath = "/Projects/" + project.getName() + "/"
                  + dataSet.getName() + "/README.md";

          try (FSDataOutputStream fsOut = udfso.create(readMeFilePath)) {
            fsOut.writeBytes(readmeFile);
            fsOut.flush();
          }
          FsPermission readmePerm = new FsPermission(FsAction.ALL,
                  FsAction.READ_EXECUTE,
                  FsAction.NONE);
          udfso.setPermission(new org.apache.hadoop.fs.Path(readMeFilePath),
                  readmePerm);
        }
      }

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
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        udfso.close();
      }
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
                "You can not create a folder inside a view-only dataset.");
      }
    }
    StringBuilder dsRelativePath = new StringBuilder();
    for (String s : datasetRelativePathArray) {
      dsRelativePath.append(s).append("/");
    }
    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      dfso = dfs.getDfsOps();
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      if (username != null) {
        udfso = dfs.getDfsOps(username);
      }
      datasetController.createSubDirectory(user, projectFacade.findByName(
              newPath.split(File.separator)[1]), fullPathArray[2],
              dsRelativePath.toString(), dataSetName.getTemplate(), dataSetName.
              getDescription(), dataSetName.isSearchable(), dfso, udfso);
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
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        udfso.close();
      }
    }
    json.setSuccessMessage("A directory was created at " + dsPath);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @DELETE
  @Path("/{fileName}")
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
                "You can not perform this action on a shared dataset.");
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
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();// do it as super user
      success = datasetController.
              deleteDataset(dataset, filePath, user, project, dfso);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not delete the file " + filePath);
    } catch (IOException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not delete the file at " + filePath);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
    if (!success) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not delete the file at " + filePath);
    }
    //remove the group associated with this dataset if the dataset is toplevel ds 
    if (filePath.endsWith(this.dataset.getInode().getInodePK().getName())) {
      try {
        hdfsUsersBean.deleteDatasetGroup(this.dataset);
      } catch (IOException ex) {
        //FIXME: take an action?
        logger.log(Level.WARNING,
                "Error while trying to delete a dataset group", ex);
      }
    }
    json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @DELETE
  @Path("file/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removefile(
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
    if (filePath.endsWith(this.dataset.getInode().getInodePK().getName())) {
      logger.log(Level.WARNING,
              "Use DELETE /{datasetName} to delete top level dataset.");
    }
    //if the path does not contain this project name it is shared.
    if (!pathArray[2].equals(this.project.getName())) { // /Projects/project/ds
      if (pathArray.length > 4 && !this.dataset.isEditable()) {// a folder in the dataset
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not perform this action on a shared dataset.");
      }
    }
    DistributedFileSystemOps udfso = null;
    try {
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      success = datasetController.
              deleteDataset(dataset, filePath, user, project, udfso);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not delete the file " + filePath);
    } catch (IOException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not delete the file at " + filePath);
    } finally {
      if (udfso != null) {
        udfso.close();
      }
    }
    if (!success) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not delete the file at " + filePath);
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
    String sourcePath = inodes.getPath(sourceInode);
    String destProject = "";
    String destDir = dto.getDestPath();
    //If destDir does not start with /Project, moving is in the same project
    if (!destDir.startsWith("/Projects/")) {
      destDir = "/Projects/" + project.getName() + "/" + destDir;
    }
    if (destDir.startsWith("/Projects/") && sourcePath.startsWith("/Projects/")) {
      destDir = destDir.replace("/Projects/", "");
      destProject = destDir.substring(0, destDir.indexOf("/"));
      destDir = destDir.substring(destDir.indexOf("/")).replaceFirst("/", "");
      sourcePath = sourcePath.replace("/Projects/", "");
      String srcProject = sourcePath.substring(0, sourcePath.indexOf("/"));
      //Do not allow copying from a shared dataset into another project
      if (!destProject.equals(srcProject)) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Cannot move file/folder from another project.");
      }
    }

    if (!destProject.equals(this.project.getName())) {
      destDir = destProject + Settings.SHARED_FILE_SEPARATOR + destDir;
    }

    destDir = getFullPath(destDir);
    DistributedFileSystemOps udfso = null;
    //We need super-user(glassfish) to change owner 
    DistributedFileSystemOps dfso = null;
    try {
      udfso = dfs.getDfsOps(username);
      dfso = dfs.getDfsOps();
      boolean exists = udfso.exists(destDir);

      //Get destination folder permissions
      String destPathParent = destDir.substring(0, destDir.lastIndexOf(
              File.separator));
      FsPermission permission = new FsPermission(inodes.getInodeAtPath(
              destPathParent).getPermission());
      org.apache.hadoop.fs.Path destPath
              = new org.apache.hadoop.fs.Path(destDir);
      String owner = udfso.getFileStatus(new org.apache.hadoop.fs.Path(inodes.
              getPath(sourceInode))).getOwner();

      udfso.moveWithinHdfs(new org.apache.hadoop.fs.Path(
              inodes.getPath(sourceInode)), destPath);

      Inode destInode = inodes.getInodeAtPath(destDir);
      String group = dfso.getFileStatus(new org.apache.hadoop.fs.Path(
              destPathParent)).getGroup();

      //Set permissions
      if (udfso.isDir(destDir)) {
        org.apache.hadoop.fs.Path parentPath = new org.apache.hadoop.fs.Path(
                destDir);

        udfso.setPermission(parentPath, permission);
        dfso.setOwner(parentPath, owner, group);

        List<Inode> children = new ArrayList<>();
        inodes.getAllChildren(destInode, children);
        for (Inode child : children) {
          org.apache.hadoop.fs.Path childPath = new org.apache.hadoop.fs.Path(
                  inodes.getPath(child));
          udfso.setPermission(childPath, permission);
          //Set group as well
          dfso.setOwner(childPath, owner, group);
        }
      } else {
        udfso.setPermission(destPath, new FsPermission(permission));
        dfso.setOwner(destPath, owner, group);
      }
      String message = "";
      JsonResponse response = new JsonResponse();

      //if it exists and it's not a dir, it must be a file
      if (exists) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Destination already exists.");
      }

      message = "Moved";
      response.setSuccessMessage(message);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Move at path:" + destDir
              + " failed. It is not a directory or you do not have permission to"
              + " move in this folder");
    } finally {
      if (udfso != null) {
        udfso.close();
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
    String sourcePath = inodes.getPath(sourceInode);
    String destProject = "";
    String destDir = dto.getDestPath();
    if (destDir.startsWith("/Projects/") && sourcePath.startsWith("/Projects/")) {
      destDir = destDir.replace("/Projects/", "");
      destProject = destDir.substring(0, destDir.indexOf("/"));
      destDir = destDir.substring(destDir.indexOf("/")).replaceFirst("/", "");
      sourcePath = sourcePath.replace("/Projects/", "");
      String srcProject = sourcePath.substring(0, sourcePath.indexOf("/"));
      //Do not allow copying from a shared dataset into another project
      if (!destProject.equals(srcProject)) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Cannot copy file/folder from another project.");
      }
    }

    if (!destProject.equals(this.project.getName())) {
      destDir = destProject + Settings.SHARED_FILE_SEPARATOR + destDir;
    }

    destDir = getFullPath(destDir);
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(username);
      boolean exists = udfso.exists(destDir);

      //Get destination folder permissions
      FsPermission permission = new FsPermission(inodes.getInodeAtPath(destDir.
              substring(0, destDir.lastIndexOf(File.separator))).getPermission());
      if (sourceInode == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Cannot find file/folder you are trying to copy. Has it been deleted?");
      }
      org.apache.hadoop.fs.Path destPath
              = new org.apache.hadoop.fs.Path(destDir);
      udfso.copyInHdfs(
              new org.apache.hadoop.fs.Path(inodes.getPath(sourceInode)),
              destPath);
      //Set permissions
      if (udfso.isDir(destDir)) {
        udfso.setPermission(destPath, permission);
        Inode destInode = inodes.getInodeAtPath(destDir);
        List<Inode> children = new ArrayList<>();
        inodes.getAllChildren(destInode, children);
        for (Inode child : children) {
          udfso.setPermission(new org.apache.hadoop.fs.Path(inodes.
                  getPath(child)), permission);
        }
      } else {
        udfso.setPermission(destPath, new FsPermission(permission));
      }
      String message = "";
      JsonResponse response = new JsonResponse();

      //if it exists and it's not a dir, it must be a file
      if (exists) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Destination already exists.");
      }

      message = "Copied";
      response.setSuccessMessage(message);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Copy at path:" + destDir
              + " failed. It is not a directory or you do not have permission to "
              + "copy in this folder");
    } finally {
      if (udfso != null) {
        udfso.close();
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
    if (path == null) {
      path = "";
    }
    path = getFullPath(path);
    DistributedFileSystemOps udfso = null;
    FSDataInputStream is = null;
    try {
      udfso = dfs.getDfsOps(username);
      boolean exists = udfso.exists(path);

      //check if the path is a file only if it exists
      if (!exists || udfso.isDir(path)) {
        throw new IOException("The file does not exist");
      }
      //tests if the user have permission to access this path
      is = udfso.open(path);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not download the file ");
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + path);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Error while closing stream.", ex);
        }
      }
      if (udfso != null) {
        udfso.close();
      }
    }
    Response.ResponseBuilder response = Response.ok();
    return response.build();
  }

  @GET
  @Path("checkFileForDownload/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response checkFileForDownload(@PathParam("path") String path,
          @Context SecurityContext sc) throws
          AppException, AccessControlException {
    return checkFileExists(path, sc);
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
    if (path == null) {
      path = "";
    }
    path = getFullPath(path);
    DistributedFileSystemOps udfso = null;
    FSDataInputStream is = null;

    JsonResponse json = new JsonResponse();
    try {
      udfso = dfs.getDfsOps(username);
      boolean exists = udfso.exists(path);

      //check if the path is a file only if it exists
      if (!exists || udfso.isDir(path)) {
        //Return an appropriate response if looking for README
        if (path.endsWith("README.md")) {
          return noCacheResponse.getNoCacheResponseBuilder(
                  Response.Status.NOT_FOUND).
                  entity(json).build();
        }
        throw new IOException("The file does not exist");
      }
      //tests if the user have permission to access this path
      is = udfso.open(path);

      //Get file type first. If it is not a known image type, display its 
      //binary contents instead
      //Set the default file type
      String fileExtension = "txt";
      //Check if file contains a valid image extension 
      if (path.contains(".")) {
        fileExtension = path.substring(path.lastIndexOf(".")).replace(".", "").
                toUpperCase();
      }
      FilePreviewDTO filePreviewDTO = null;
      //If it is an image smaller than 10MB download it
      //otherwise thrown an error
      if (HopsUtils.isInEnum(fileExtension, FilePreviewImageTypes.class)) {
        int imageSize = (int) udfso.getFileStatus(new org.apache.hadoop.fs.Path(
                path)).getLen();
        if (udfso.getFileStatus(new org.apache.hadoop.fs.Path(path)).getLen()
                < settings.getFilePreviewImageSize()) {
          //Read the image in bytes and convert it to base64 so that is 
          //rendered properly in the front-end
          byte[] imageInBytes = new byte[imageSize];
          is.readFully(imageInBytes);
          String base64Image = new Base64().encodeAsString(imageInBytes);
          filePreviewDTO = new FilePreviewDTO("image",
                  fileExtension.toLowerCase(), base64Image);
          json.setData(filePreviewDTO);
        } else {
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  "Image at " + path
                  + " is too big to display, please download it by double-clicking it instead");
        }
      } else {
        long fileSize = udfso.getFileStatus(new org.apache.hadoop.fs.Path(
                path)).getLen();
        DataInputStream dis = new DataInputStream(is);
        try {
          //If file is less thatn 512KB, preview it
          int sizeThreshold = Settings.FILE_PREVIEW_TXT_SIZE_BYTES; //in bytes
          if (fileSize > sizeThreshold && !path.endsWith("README.md")) {
            if (mode.equals("tail")) {
              dis.skipBytes((int) (fileSize - sizeThreshold));
            }
            try {
              byte[] headContent = new byte[sizeThreshold];
              dis.readFully(headContent, 0, sizeThreshold);
              //File content
              filePreviewDTO = new FilePreviewDTO("text", fileExtension.
                      toLowerCase(), new String(headContent));
            } catch (IOException ex) {
              logger.log(Level.SEVERE, ex.getMessage());
            }
          } else if (fileSize > sizeThreshold && path.endsWith("README.md")
                  && fileSize > Settings.FILE_PREVIEW_TXT_SIZE_BYTES_README) {
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "README.md must be smaller than "
                    + Settings.FILE_PREVIEW_TXT_SIZE_BYTES_README
                    + " to be previewd");
          } else {
            byte[] headContent = new byte[(int) fileSize];
            dis.readFully(headContent, 0, (int) fileSize);
            //File content
            filePreviewDTO = new FilePreviewDTO("text", fileExtension.
                    toLowerCase(), new String(headContent));
          }

          json.setData(filePreviewDTO);
        } finally {
          dis.close();
        }
      }
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not view the file ");
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + path);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Error while closing stream.", ex);
        }
      }
      if (udfso != null) {
        udfso.close();
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

    if (path == null) {
      path = "";
    }
    path = getFullPath(path);
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      boolean exists = dfso.exists(path);
      boolean isDir = dfso.isDir(path);

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
    } finally {
      if (dfso != null) {
        dfso.close();
      }
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
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      String blocks = dfso.getFileBlocks(path);

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(blocks).build();

    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + path);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
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
      if (!this.dataset.isEditable() && !this.dataset.isPublicDs()) {
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

    Users user = this.userFacade.findByEmail(context.getUserPrincipal().
            getName());

    ErasureCodeJobConfiguration ecConfig
            = (ErasureCodeJobConfiguration) JobConfiguration.JobConfigurationFactory.
            getJobConfigurationTemplate(JobType.ERASURE_CODING);
    ecConfig.setFilePath(path);

    //persist the job in the database
    JobDescription jobdesc = this.jobcontroller.createJob(user, project,
            ecConfig);
    //instantiate the job
    ErasureCodeJob encodeJob = new ErasureCodeJob(jobdesc, this.async, user,
            settings.getHadoopDir(), jobsMonitor);
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
    this.uploader.setIsTemplate(false);
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
      Inode dsInode = inodes.findByInodePK(parent, dsName,
              HopsUtils.dataSetPartitionId(parent, dsName));
      this.dataset = datasetFacade.findByProjectAndInode(this.project, dsInode);
      if (this.dataset == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.DATASET_NOT_FOUND);
      }
      if (this.dataset.getStatus() == Dataset.PENDING) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Dataset is not yet accessible. Accept the share request to access it.");
      }
      path = path.replaceFirst(projectName + Settings.SHARED_FILE_SEPARATOR
              + dsName, projectName
              + File.separator + dsName);
    } else if (parts != null) {
      dsName = parts[0];
      Inode parent = inodes.getProjectRoot(this.project.getName());
      Inode dsInode = inodes.findByInodePK(parent, dsName, HopsUtils.
              dataSetPartitionId(parent, dsName));
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

  @GET
  @Path("/makePublic/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response makePublic(@PathParam("inodeId") Integer inodeId,
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
    if (ds.isPublicDs()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_ALREADY_PUBLIC);
    }
    ds.setPublicDs(true);
    datasetFacade.merge(ds);
    datasetController.logDataset(ds, OperationType.Update);
    json.setSuccessMessage("The Dataset is now public.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/removePublic/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removePublic(@PathParam("inodeId") Integer inodeId,
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
    if (ds.isPublicDs() == false) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_PUBLIC);
    }
    ds.setPublicDs(false);
    datasetFacade.merge(ds);
    datasetController.logDataset(ds, OperationType.Update);
    json.setSuccessMessage("The Dataset is no longer public.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

}
