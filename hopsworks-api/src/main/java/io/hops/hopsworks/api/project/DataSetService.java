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

package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.util.DsDTOValidator;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.DsUpdateOperations;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.api.util.DownloadService;
import io.hops.hopsworks.api.util.FilePreviewImageTypes;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.api.util.UploadService;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.DataSetDTO;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetPermissions;
import io.hops.hopsworks.common.dao.dataset.DatasetRequest;
import io.hops.hopsworks.common.dao.dataset.DatasetRequestFacade;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.dao.metadata.db.TemplateFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiScope;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.hdfs.FsPermissions;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DataSetService {

  private static final Logger LOGGER = Logger.getLogger(DataSetService.class.
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
  private HdfsUsersController hdfsUsersController;
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
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private DsUpdateOperations dsUpdateOperations;

  private Integer projectId;
  private Project project;

  public DataSetService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  @GET
  @Path("unzip/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unzip(@PathParam("path") String path, @Context SecurityContext sc) throws DatasetException,
    ProjectException {

    Response.Status resp = Response.Status.OK;
    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();
    // HDFS_USERNAME is the next param to the bash script
    Users user = jWTHelper.getUserPrincipal(sc);
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    datasetController.checkFileExists(dsPath.getFullPath(), hdfsUser);
    String localDir = DigestUtils.sha256Hex(fullPath);
    String stagingDir = settings.getStagingDir() + File.separator + localDir;

    File unzipDir = new File(stagingDir);
    unzipDir.mkdirs();
    settings.addUnzippingState(fullPath);

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(settings.getHopsworksDomainDir() + "/bin/unzip-background.sh")
        .addCommand(stagingDir)
        .addCommand(fullPath)
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
          "path: " + fullPath + ", result: " + result);
      }
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_ERROR, Level.SEVERE,
        "path: " + fullPath, ex.getMessage(), ex);
    }

    return noCacheResponse.getNoCacheResponseBuilder(resp).build();
  }

  @GET
  @Path("zip/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response zip(@PathParam("path") String path, @Context SecurityContext sc) throws DatasetException,
    ProjectException {

    Response.Status resp = Response.Status.OK;
    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();
    // HDFS_USERNAME is the next param to the bash script
    Users user = jWTHelper.getUserPrincipal(sc);
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
    datasetController.checkFileExists(dsPath.getFullPath(), hdfsUser);
    String localDir = DigestUtils.sha256Hex(fullPath);
    String stagingDir = settings.getStagingDir() + File.separator + localDir;

    File zipDir = new File(stagingDir);
    zipDir.mkdirs();
    settings.addZippingState(fullPath);

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand(settings.getHopsworksDomainDir() + "/bin/zip-background.sh")
        .addCommand(stagingDir)
        .addCommand(fullPath)
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
          "path: " + fullPath + ", result: " + result);
      }
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_ERROR, Level.SEVERE,
        "path: " + fullPath, ex.getMessage(), ex);
    }

    return noCacheResponse.getNoCacheResponseBuilder(resp).build();
  }

  @GET
  @Path("/getContent/")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response findDataSetsInProjectID() {

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
   * @return
   * @throws DatasetException
   * @throws ProjectException
   */
  @GET
  @Path("/getContent/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getDirContent(@PathParam("path") String path) throws DatasetException, ProjectException {
    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();
    Inode parent = dsPath.validatePathExists(inodes,true);
    List<Inode> cwdChildren = inodes.getChildren(parent);

    List<InodeView> kids = new ArrayList<>();
    for (Inode i : cwdChildren) {
      InodeView inodeView = new InodeView(i, fullPath + "/" + i.getInodePK().getName());
      if (dsPath.getDs().isShared()) {
        //Get project of project__user the inode is owned by
        inodeView.setOwningProjectName(hdfsUsersController.getProjectName(i.getHdfsUser().getName()));
      }
      inodeView.setZipState(settings.getZipState(
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getFile(@PathParam("path") String path) throws DatasetException, ProjectException {

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    // The inode can be both a file and a directory
    String fullPath = dsPath.getFullPath().toString();
    Inode inode = dsPath.validatePathExists(inodes,null);

    InodeView inodeView = new InodeView(inode, fullPath+ "/" + inode.getInodePK().
            getName());
    inodeView.setZipState(settings.getZipState(
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response shareDataSet(DataSetDTO dataSet, @Context SecurityContext sc) throws DatasetException {

    Users user = jWTHelper.getUserPrincipal(sc);
    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, false);
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    // Check target project
    Project proj = projectFacade.find(dataSet.getProjectId());

    Dataset dst = datasetFacade.findByProjectAndInode(proj, ds.getInode());
    if (dst != null) {//proj already have the dataset.
      throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS,
        Level.FINE, "Dataset already in " + proj.getName());
    }

    // Create the new Dataset entry
    Dataset newDS = new Dataset(ds, proj);
    newDS.setShared(true);

    // if the dataset is not requested or is requested by a data scientist
    // set status to pending.
    DatasetRequest dsReq = datasetRequest.findByProjectAndDataset(proj, ds);
    if (dsReq == null || dsReq.getProjectTeam().getTeamRole().equals(
            AllowedProjectRoles.DATA_SCIENTIST)) {
      newDS.setStatus(Dataset.PENDING);
    } else {
      hdfsUsersController.shareDataset(proj, ds);
    }

    datasetFacade.persistDataset(newDS);

    if (dsReq != null) {
      datasetRequest.remove(dsReq);//the dataset is shared so remove the request.
    }

    activityFacade.persistActivity(ActivityFacade.SHARED_DATA + dataSet.getName() + " with project " + proj.getName(), 
        project, user, ActivityFlag.DATASET);

    json.setSuccessMessage("The Dataset was successfully shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/unshareDataSet")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response unshareDataSet(DataSetDTO dataSet, @Context SecurityContext sc) throws DatasetException {

    Users user = jWTHelper.getUserPrincipal(sc);
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, true);

    for (int id : dataSet.getProjectIds()) {
      Project proj = projectFacade.find(id);
      Dataset dst = datasetFacade.findByProjectAndInode(proj, ds.getInode());
      if (dst == null) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_SHARED_WITH_PROJECT, Level.FINE,
          "project: " + proj.getName());
      }
      hdfsUsersController.unshareDataset(proj, ds);
      datasetFacade.removeDataset(dst);
      activityFacade.persistActivity(ActivityFacade.UNSHARED_DATA + dataSet.getName() + " with project " + 
          proj.getName(), project, user, ActivityFlag.DATASET);
    }
    json.setSuccessMessage("The Dataset was successfully unshared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/projectsSharedWith")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getProjectSharedWith(DataSetDTO dataSet) throws DatasetException {

    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, true);

    List<Project> list = datasetFacade.findProjectSharedWith(project, ds.getInode());
    GenericEntity<List<Project>> projects = new GenericEntity<List<Project>>(
            list) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projects).build();
  }

  @ApiOperation(value = "Set permissions (potentially with sticky bit) for datasets",
      notes = "Allow data scientists to create and modify own files in dataset.")
  @PUT
  @Path("/permissions")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response setPermissions(DataSetDTO dataSet) throws DatasetException {
    Dataset ds = dtoValidator.validateDTO(this.project, dataSet, false);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      // Change permission as super user
      FsPermission fsPermission = null;
      if (null != dataSet.getPermissions()) {
        switch (dataSet.getPermissions()) {
          case OWNER_ONLY:
            fsPermission = FsPermissions.rwxr_x___;
            ds.setEditable(DatasetPermissions.OWNER_ONLY);
            break;
          case GROUP_WRITABLE_SB:
            fsPermission = FsPermissions.rwxrwx___T;
            ds.setEditable(DatasetPermissions.GROUP_WRITABLE_SB);
            break;
          case GROUP_WRITABLE:
            fsPermission = FsPermissions.rwxrwx___;
            ds.setEditable(DatasetPermissions.GROUP_WRITABLE);
            break;
          default:
            break;
        }
        datasetController.recChangeOwnershipAndPermission(datasetController.getDatasetPath(ds), fsPermission, null,
            null,null, dfso);
        datasetController.changePermissions(ds);
      }
    } catch (IOException e) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_PERMISSION_ERROR, Level.WARNING,
        "dataset: " + ds.getId(), e.getMessage(), e);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }

    json.setSuccessMessage("The Dataset permissions were successfully modified.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @GET
  @Path("/accept/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response acceptRequest(@PathParam("inodeId") Long inodeId) throws DatasetException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (inodeId == null) {
      throw new IllegalArgumentException("inodeId was not provided");
    }
    Inode inode = inodes.findById(inodeId);
    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);
    hdfsUsersController.shareDataset(this.project, ds);
    ds.setStatus(Dataset.ACCEPTED);
    datasetFacade.merge(ds);
    json.setSuccessMessage("The Dataset is now accessable.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/reject/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response rejectRequest(@PathParam("inodeId") Long inodeId) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (inodeId == null) {
      throw new IllegalArgumentException("inodeId was not provided.");
    }
    Inode inode = inodes.findById(inodeId);
    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);

    datasetFacade.remove(ds);
    json.setSuccessMessage("The Dataset has been removed.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/createTopLevelDataSet")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response createTopLevelDataSet(DataSetDTO dataSet, @Context SecurityContext sc)
    throws DatasetException, HopsSecurityException {

    Users user = jWTHelper.getUserPrincipal(sc);
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    String username = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(username);

    try {
      datasetController.createDataset(user, project, dataSet.getName(),
        dataSet.getDescription(), dataSet.getTemplate(), dataSet.isSearchable(),
        false, false, dfso);
      //Generate README.md for the dataset if the user requested it
      if (dataSet.isGenerateReadme()) {
        //Persist README.md to hdfs
        datasetController.generateReadme(udfso, dataSet.getName(), dataSet.getDescription(),
          project.getName());
      }
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("The Dataset was created successfully.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response createDataSetDir(DataSetDTO dataSetName, @Context SecurityContext sc) throws DatasetException,
      HopsSecurityException, ProjectException {

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);

    org.apache.hadoop.fs.Path fullPath =
        dsUpdateOperations.createDirectoryInDataset(
            this.project, user, dataSetName.getName(), dataSetName.getDescription(),
        dataSetName.getTemplate(), dataSetName.isSearchable());
    json.setSuccessMessage("A directory was created at " + fullPath);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  /**
   * This function is used only for deletion of dataset directories
   * as it does not accept a path
   * @param fileName
   * @param sc
   * @return
   * @return
   * @throws DatasetException
   * @throws ProjectException
   */
  @DELETE
  @Path("/{fileName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_DELETE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response removedataSetdir(@PathParam("fileName") String fileName, @Context SecurityContext sc) throws
      DatasetException, ProjectException {
    boolean success = false;
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    DsPath dsPath = pathValidator.validatePath(this.project, fileName);
    Dataset ds = dsPath.getDs();
    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();

    if (ds.isShared()) {
      // The user is trying to delete a dataset. Drop it from the table
      // But leave it in hopsfs because the user doesn't have the right to delete it
      hdfsUsersController.unShareDataset(project, ds);
      datasetFacade.removeDataset(ds);
      json.setSuccessMessage(ResponseMessages.SHARED_DATASET_REMOVED);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json).build();
    }

    DistributedFileSystemOps dfso = null;
    try {
      Users user = jWTHelper.getUserPrincipal(sc);
      String username = hdfsUsersController.getHdfsUserName(project, user);
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
      success = datasetController.
              deleteDatasetDir(ds, fullPath, dfso);
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.SEVERE,
        "path: " + fullPath.toString(),
        ex.getMessage(), ex);
    }  finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }

    if (!success) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.FINE,
        "path: " + fullPath.toString());
    }

    //remove the group associated with this dataset as it is a toplevel ds
    try {
      hdfsUsersController.deleteDatasetGroup(ds);
    } catch (IOException ex) {
      //FIXME: take an action?
      LOGGER.log(Level.WARNING,
              "Error while trying to delete a dataset group", ex);
    }
    json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }
  
  /**
   * Removes corrupted files from incomplete downloads.
   * @param fileName
   * @param sc
   * @return
   */
  @DELETE
  @Path("corrupted/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removeCorrupted(@PathParam("fileName") String fileName, @Context SecurityContext sc) throws
      DatasetException, ProjectException {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);

    DsPath dsPath = pathValidator.validatePath(this.project, fileName);
    Dataset ds = dsPath.getDs();

    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    org.apache.hadoop.fs.Path dsRelativePath = dsPath.getDsRelativePath();

    if (dsRelativePath.depth() == 0) {
      throw new IllegalArgumentException("Use DELETE /{datasetName} to delete top level dataset)");
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
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.SEVERE,
        "path: " + fullPath.toString(), ex.getMessage(), ex);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }

    throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.FINE,
      "path: " + fullPath.toString());

  }

  /**
   * Differently from the previous function, this accepts a path.
   * If it is used to delete a dataset directory it will throw an exception
   * (line 779)
   * @param fileName
   * @param sc
   * @return
   * @throws DatasetException
   * @throws ProjectException
   */
  @DELETE
  @Path("file/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_DELETE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response removefile(@PathParam("fileName") String fileName, @Context SecurityContext sc) throws
      DatasetException, ProjectException {
    boolean success = false;
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);

    DsPath dsPath = pathValidator.validatePath(this.project, fileName);
    Dataset ds = dsPath.getDs();

    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    org.apache.hadoop.fs.Path dsRelativePath = dsPath.getDsRelativePath();

    if (dsRelativePath.depth() == 0) {
      throw new IllegalArgumentException("Use DELETE /{datasetName} to delete top level dataset)");
    }

    DistributedFileSystemOps dfso = null;
    try {
      String username = hdfsUsersController.getHdfsUserName(project, user);
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
    json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  /**
   * Move and Rename operations handled here
   *
   * @param sc
   * @param dto
   * @return
   * @throws DatasetException
   * @throws ProjectException
   */
  @POST
  @Path("move")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE, ApiScope.DATASET_DELETE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response moveFile(@Context SecurityContext sc, MoveDTO dto) throws DatasetException, ProjectException,
      HopsSecurityException {
    Users user = jWTHelper.getUserPrincipal(sc);

    Inode sourceInode = inodes.findById(dto.getInodeId());
    dsUpdateOperations.moveDatasetFile(project, user, sourceInode, dto.getDestPath());
    RESTApiJsonResponse response = new RESTApiJsonResponse();
    response.setSuccessMessage("Moved");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(response).build();
  }

  /**
   * Copy operations handled here.
   *
   * @param sc
   * @param dto
   * @return
   * @throws DatasetException
   * @throws ProjectException
   */
  @POST
  @Path("copy")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response copyFile(@Context SecurityContext sc, MoveDTO dto) throws DatasetException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    String username = hdfsUsersController.getHdfsUserName(project, user);
    Inode sourceInode = inodes.findById(dto.getInodeId());
    String sourcePathStr = inodes.getPath(sourceInode);

    DsPath sourceDsPath = pathValidator.validatePath(this.project, sourcePathStr);
    DsPath destDsPath = pathValidator.validatePath(this.project, dto.getDestPath());

    Dataset sourceDataset = sourceDsPath.getDs();
    // The destination dataset project is already the correct one, as the
    // full path is given in the MoveDTO object
    Dataset destDataset = destDsPath.getDs();

    if (!datasetController.getOwningProject(sourceDataset).equals(
        destDataset.getProject())) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.COPY_FROM_PROJECT, Level.FINE);
    }

    if (destDataset.isPublicDs()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.COPY_TO_PUBLIC_DS, Level.FINE);
    }

    org.apache.hadoop.fs.Path sourcePath = sourceDsPath.getFullPath();
    org.apache.hadoop.fs.Path destPath = destDsPath.getFullPath();

    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(username);

      if (udfso.exists(destPath.toString())){
        throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE);
      }

      //Get destination folder permissions
      FsPermission permission = udfso.getFileStatus(destPath.getParent()).getPermission();
      udfso.copyInHdfs(sourcePath, destPath);

      //Set permissions
      datasetController.recChangeOwnershipAndPermission(destPath, permission,
          null, null, null, udfso);

      RESTApiJsonResponse response = new RESTApiJsonResponse();
      response.setSuccessMessage("Copied");

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();

    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.SEVERE, "move operation " +
        "failed for: " + sourcePathStr, ex.getMessage(), ex);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  @GET
  @Path("fileExists/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response checkFileExists(@PathParam("path") String path, @Context SecurityContext sc) throws DatasetException,
    ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    String username = hdfsUsersController.getHdfsUserName(project, user);
    DsPath dsPath = pathValidator.validatePath(this.project, path);
    dsPath.validatePathExists(inodes, false);
    org.apache.hadoop.fs.Path filePath = dsPath.getFullPath();

    datasetController.checkFileExists(filePath, username);
    Response.ResponseBuilder response = Response.ok();
    return response.build();
  }

  @GET
  @Path("checkFileForDownload/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response checkFileForDownload(@PathParam("path") String path,
          @Context SecurityContext sc) throws DatasetException, ProjectException {
    if(!settings.isDownloadAllowed()){
      throw new DatasetException(RESTCodes.DatasetErrorCode.DOWNLOAD_NOT_ALLOWED, Level.FINEST);
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    DsPath dsPath = pathValidator.validatePath(this.project, path);
    Project owningProject = datasetController.getOwningProject(dsPath.getDs());
    RESTApiJsonResponse response = new RESTApiJsonResponse();
    String username = hdfsUsersController.getHdfsUserName(project, user);
    //User must be accessing a dataset directly, not by being shared with another project.
    //For example, DS1 of project1 is shared with project2. User must be a member of project1 to download files
    if (owningProject.equals(project) && datasetController.isDownloadAllowed(project, user, dsPath.getFullPath().
        toString())) {
      datasetController.checkFileExists(dsPath.getFullPath(), username);
      String token = jWTHelper.createOneTimeToken(user, dsPath.getFullPath().toString(), null);
      if (token != null && !token.isEmpty()) {
        response.setData(token);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
      }
    }
    response.setErrorMsg(ResponseMessages.DOWNLOAD_PERMISSION_ERROR);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.FORBIDDEN).entity(response).build();
  }

  @GET
  @Path("filePreview/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response filePreview(@PathParam("path") String path, @QueryParam("mode") String mode,
          @Context SecurityContext sc) throws DatasetException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    String username = hdfsUsersController.getHdfsUserName(project, user);

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    dsPath.validatePathExists(inodes,false);
    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
    String fileName = fullPath.getName();

    DistributedFileSystemOps udfso = null;
    FSDataInputStream is = null;

    RESTApiJsonResponse json = new RESTApiJsonResponse();
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
  
      FilePreviewDTO filePreviewDTO = null;
      if (HopsUtils.isInEnum(fileExtension, FilePreviewImageTypes.class)) {
        //If it is an image smaller than 10MB download it otherwise thrown an error
        if (fileSize < settings.getFilePreviewImageSize()) {
          //Read the image in bytes and convert it to base64 so that is
          //rendered properly in the front-end
          byte[] imageInBytes = new byte[(int) fileSize];
          is.readFully(imageInBytes);
          String base64Image = new Base64().encodeAsString(imageInBytes);
          filePreviewDTO = new FilePreviewDTO(Settings.FILE_PREVIEW_IMAGE_TYPE,
            fileExtension.toLowerCase(), base64Image);
        } else {
          throw new DatasetException(RESTCodes.DatasetErrorCode.IMAGE_SIZE_INVALID, Level.FINE);
        }
      } else {
        try (DataInputStream dis = new DataInputStream(is)) {
          int sizeThreshold = Settings.FILE_PREVIEW_TXT_SIZE_BYTES; //in bytes
          if (fileSize > sizeThreshold && !fileName.endsWith(Settings.README_FILE)
            && mode.equals(Settings.FILE_PREVIEW_MODE_TAIL)) {
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
  
      json.setData(filePreviewDTO);
    } catch (AccessControlException ae) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_ACCESS_PERMISSION_DENIED, Level.SEVERE,
        "path: " + path, ae.getMessage(), ae);
    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.SEVERE, "path: " + path,
        ex.getMessage(), ex);
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

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("isDir/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response isDir(@PathParam("path") String path) throws DatasetException, ProjectException {

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    Inode inode = dsPath.validatePathExists(inodes, null);

    RESTApiJsonResponse response = new RESTApiJsonResponse();
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response countFileBlocks(@PathParam("path") String path) throws DatasetException, ProjectException {

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();

    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      String blocks = dfso.getFileBlocks(fullPath);

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(blocks).build();

    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.SEVERE, "path: " + path,
        ex.getMessage(), ex);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  @Path("fileDownload")
  public DownloadService downloadDS() {
    this.downloader.setProject(project);
    return this.downloader;
  }

//  @Path("compressFile/{path: .+}")
//  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
//  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
//  public Response compressFile(@PathParam("path") String path, @Context SecurityContext sc)
//    throws JobException, DatasetException, ProjectException {
//    Users user = jWTHelper.getUserPrincipal(sc);
//
//    DsPath dsPath = pathValidator.validatePath(this.project, path);
//    org.apache.hadoop.fs.Path fullPath = dsPath.getFullPath();
//    Dataset ds = dsPath.getDs();
//    if (ds.isShared() && ds.getEditable() == DatasetPermissions.OWNER_ONLY && !ds.isPublicDs()) {
//      throw new DatasetException(RESTCodes.DatasetErrorCode.COMPRESSION_ERROR, Level.FINE);
//    }
//
//    ErasureCodeJobConfiguration ecConfig = (ErasureCodeJobConfiguration) JobConfiguration.JobConfigurationFactory.
//            getJobConfigurationTemplate(JobType.ERASURE_CODING);
//    ecConfig.setFilePath(fullPath.toString());
//
//    //persist the job in the database
//    Jobs jobdesc = null;
//    jobdesc = this.jobcontroller.putJob(user, project, null, ecConfig);
//    //instantiate the job
//    ErasureCodeJob encodeJob = new ErasureCodeJob(jobdesc, this.async, user,
//            settings.getHadoopSymbolicLinkDir(), jobsMonitor);
//    //persist a job execution instance in the database and get its id
//    Execution exec = encodeJob.requestExecutionId();
//      //start the actual job execution i.e. compress the file in a different thread
//    this.async.startExecution(encodeJob);
//
//    String response = "File compression runs in background";
//    RESTApiJsonResponse json = new RESTApiJsonResponse();
//    json.setSuccessMessage(response);
//    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
//            json).build();
//  }

  /**
   * Upload methods
   *
   * @param path
   * @param templateId
   * @return
   */
  @Path("upload/{path: .+}")
  public UploadService upload(@PathParam("path") String path, @QueryParam("templateId") int templateId) {
    this.uploader.setParams(project, path, templateId, false);
    return this.uploader;
  }

  @POST
  @Path("/attachTemplate")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response attachTemplate(FileTemplateDTO filetemplateData) {

    if (filetemplateData == null || filetemplateData.getInodePath() == null
            || filetemplateData.getInodePath().equals("")) {
      throw new IllegalArgumentException("filetempleateData was not provided or its InodePath was not set");
    }

    String inodePath = filetemplateData.getInodePath();
    int templateid = filetemplateData.getTemplateId();

    Inode inode = inodes.getInodeAtPath(inodePath);
    Template temp = template.findByTemplateId(templateid);
    temp.getInodes().add(inode);

    //persist the relationship
    this.template.updateTemplatesInodesMxN(temp);

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("The template was attached to file "
            + inode.getId());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }
}
