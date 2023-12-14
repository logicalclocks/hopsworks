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

package io.hops.hopsworks.api.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.api.filter.JWTNotRequired;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.LongRunningHttpRequests;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetPermissions;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.ApiOperation;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.fs.permission.FsPermission;
import org.javatuples.Pair;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DownloadService {

  private static final Logger LOGGER = Logger.getLogger(DownloadService.class.getName());

  @EJB
  private DistributedFsService dfs;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private Settings settings;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private LongRunningHttpRequests longRunningHttpRequests;

  public DownloadService() {
  }
  
  private Integer projectId;
  private String projectName;
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  private Project getProject() throws ProjectException {
    if (this.projectId != null) {
      return projectController.findProjectById(this.projectId);
    } else if (this.projectName != null) {
      return projectController.findProjectByName(this.projectName);
    }
    throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE);
  }
  
  @GET
  @javax.ws.rs.Path("token/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get a one time download token.", response = RESTApiJsonResponse.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getDownloadToken(@PathParam("path") String path,
                                   @QueryParam("type") DatasetType datasetType,
                                   @Context HttpServletRequest req,
                                   @Context SecurityContext sc) throws DatasetException, ProjectException {
    doCheck();
    Users user = jWTHelper.getUserPrincipal(sc);
    DatasetPath datasetPath = datasetHelper.getDatasetPathIfFileExist(this.getProject(), path, datasetType);
    Project owningProject = datasetController.getOwningProject(datasetPath.getDataset());
    RESTApiJsonResponse response = new RESTApiJsonResponse();
    String username = hdfsUsersController.getHdfsUserName(this.getProject(), user);
    //User must be accessing a dataset directly, not by being shared with another project.
    //For example, DS1 of project1 is shared with project2. User must be a member of project1 to download files
    if (owningProject.equals(this.getProject()) &&
      datasetController.isDownloadAllowed(this.getProject(), user, datasetPath.getFullPath().toString())) {
      datasetController.checkFileExists(datasetPath.getFullPath(), username);
      String token = jWTHelper.createOneTimeToken(user, datasetPath.getFullPath().toString(), null);
      if (token != null && !token.isEmpty()) {
        response.setData(token);
        return Response.status(Response.Status.OK).entity(response).build();
      }
    }
    response.setErrorMsg(ResponseMessages.DOWNLOAD_PERMISSION_ERROR);

    return Response.status(Response.Status.FORBIDDEN).entity(response).build();
  }

  @GET
  @javax.ws.rs.Path("with_token/{path: .+}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @JWTNotRequired
  @ApiOperation(value = "Download file.", response = StreamingOutput.class)
  public Response downloadFromHDFS(@PathParam("path") String path,
                                   @QueryParam("token") String token,
                                   @QueryParam("type") DatasetType datasetType,
                                   @Context HttpServletRequest req,
                                   @Context SecurityContext sc) throws DatasetException,
    SigningKeyNotFoundException, VerificationException, ProjectException {
    Project project = this.getProject();
    DatasetPath datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
    String fullPath = datasetPath.getFullPath().toString();
    DecodedJWT djwt = jWTHelper.verifyOneTimeToken(token, fullPath);
    Users user = userFacade.findByUsername(djwt.getSubject());
    return download(project, datasetPath, user);
  }

  @GET
  @javax.ws.rs.Path("with_auth/{path: .+}")
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @ApiOperation(value = "Download file.", response = StreamingOutput.class)
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_VIEW},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response downloadFromHDFS(@PathParam("path") String path,
                                   @QueryParam("type") DatasetType datasetType,
                                   @Context HttpServletRequest req,
                                   @Context SecurityContext sc) throws DatasetException, ProjectException {
    Project project = this.getProject();
    DatasetPath datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
    Users user = jWTHelper.getUserPrincipal(sc);
    return download(project, datasetPath, user);
  }

  private Response download(Project project, DatasetPath datasetPath, Users user) throws DatasetException {
    doCheck();
    Pair<Path, StreamingOutput> pathStreamPair = downloadFromHDFS(project, datasetPath, user);
    Response.ResponseBuilder response = Response.ok(pathStreamPair.getValue1());
    response.header("Content-disposition", "attachment; filename=\"" + pathStreamPair.getValue0().getName() + "\"");
    return response.build();
  }

  private void doCheck() throws DatasetException {
    if (!settings.isDownloadAllowed()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DOWNLOAD_NOT_ALLOWED, Level.FINEST);
    }
    if (longRunningHttpRequests.get() >= settings.getMaxLongRunningHttpRequests()) {
      LOGGER.log(Level.INFO, "The maximum number of allowed download operations exceeded. {0}",
        longRunningHttpRequests.get());
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_INVALID, Level.FINE,
        "The maximum number of allowed download operations exceeded. Please try again later.");
    }
  }

  /**
   * @param project
   * @param datasetPath
   * @param user
   * @return
   */
  private Pair<Path, StreamingOutput> downloadFromHDFS(Project project, DatasetPath datasetPath, Users user)
    throws DatasetException {

    String fullPath = datasetPath.getFullPath().toString();
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);

    Dataset ds = datasetPath.getDataset();
    Inode dsInode = datasetPath.getInode();
    FsPermission fsPermission = new FsPermission(dsInode.getPermission());

    if (!ds.isPublicDs() && ds.isShared(project) &&
      DatasetPermissions.fromFilePermissions(fsPermission).equals(DatasetPermissions.OWNER_ONLY)) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DOWNLOAD_ERROR, Level.FINE,
        "You do not have the rights to download from this dataset");
    }

    FSDataInputStream stream;
    DistributedFileSystemOps udfso;
    try {
      if (projectUsername != null) {
        udfso = dfs.getDfsOps(projectUsername);
        Path p = new Path(fullPath);
        stream = udfso.open(p);
        return new Pair<>(p, buildOutputStream(stream, udfso));

      } else {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DOWNLOAD_ERROR, Level.WARNING, "Project user not found.");
      }

    } catch (IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DOWNLOAD_ERROR, Level.SEVERE,
        "Failed to download path: " + fullPath,
        ex.getMessage(), ex);
    }
  }

  /**
   *
   * @param stream
   * @return
   */
  private StreamingOutput buildOutputStream(final FSDataInputStream stream, final DistributedFileSystemOps udfso) {
    return out -> {
      longRunningHttpRequests.increment();
      try {
        int length;
        byte[] buffer = new byte[1024];
        while ((length = stream.read(buffer)) != -1) {
          out.write(buffer, 0, length);
        }
        out.flush();
        stream.close();
      } finally {
        longRunningHttpRequests.decrement();
        dfs.closeDfsClient(udfso);
      }
    };
  }

}
