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

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.upload.FlowInfo;
import io.hops.hopsworks.common.upload.UploadController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.security.AccessControlException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UploadService {

  private static final Logger LOGGER = Logger.getLogger(UploadService.class.getName());

  @EJB
  private DatasetController datasetController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UploadController uploadController;

  private String path;
  private DatasetType datasetType;
  private String username;
  
  private Integer projectId;
  private String projectName;
  
  private Project getProjectById() throws ProjectException {
    Project project = projectFacade.find(this.projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    return project;
  }
  
  private Project getProjectByName() throws ProjectException {
    Project project = projectFacade.findByName(this.projectName);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectName: " +
        projectName);
    }
    return project;
  }
  
  private Project getProject() throws ProjectException {
    return this.projectId != null ? getProjectById() : getProjectByName();
  }

  public UploadService() {
  }
  
  public void setParams(Integer projectId, String path, DatasetType datasetType) {
    this.projectId = projectId;
    this.path = path;
    this.datasetType = datasetType;
  }
  
  public void setParams(String projectName, String path, DatasetType datasetType) {
    this.projectName = projectName;
    this.path = path;
    this.datasetType = datasetType;
  }
  
  private void configureUploader(SecurityContext sc) throws DatasetException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project =this.getProject();
    this.username = hdfsUsersBean.getHdfsUserName(project, user);
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, this.datasetType);
    this.path = datasetPath.getFullPath().toString();
  }

  private void validate(FlowInfo flowInfo) throws DatasetException {
    if (!flowInfo.valid()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_RESUMABLEINFO_INVALID, Level.FINE, "Missing " +
        "arguments for upload.");
    }
    if (flowInfo.getTotalChunks() > 10000) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_RESUMABLEINFO_INVALID, Level.FINE, "File too big " +
        "for upload. Try using bigger chunks.");
    }
  }

  @GET
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_CREATE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response testMethod(@QueryParam("flowChunkNumber") String flowChunkNumber,
      @QueryParam("flowChunkSize") String flowChunkSize,
      @QueryParam("flowCurrentChunkSize") String flowCurrentChunkSize,
      @QueryParam("flowFilename") String flowFilename,
      @QueryParam("flowIdentifier") String flowIdentifier,
      @QueryParam("flowRelativePath") String flowRelativePath,
      @QueryParam("flowTotalChunks") String flowTotalChunks,
      @QueryParam("flowTotalSize") String flowTotalSize,
      @Context HttpServletRequest request, @Context SecurityContext sc) throws IOException, DatasetException,
      ProjectException {
    configureUploader(sc);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    FlowInfo flowInfo = new FlowInfo(
      request.getParameter("flowChunkNumber"),
      request.getParameter("flowChunkSize"),
      request.getParameter("flowCurrentChunkSize"),
      request.getParameter("flowFilename"),
      request.getParameter("flowIdentifier"),
      request.getParameter("flowRelativePath"),
      request.getParameter("flowTotalChunks"),
      request.getParameter("flowTotalSize"));
    validate(flowInfo);

    if (uploadController.uploaded(flowInfo, this.path)) {
      json.setSuccessMessage("Uploaded");
      return Response.status(Response.Status.OK).entity(json).build();
    } else {
      json.setErrorMsg("Not uploaded");
      return Response.status(Response.Status.NO_CONTENT).entity(json).build();
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_CREATE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response uploadMethod(
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("flowChunkNumber") String flowChunkNumber,
      @FormDataParam("flowChunkSize") String flowChunkSize,
      @FormDataParam("flowCurrentChunkSize") String flowCurrentChunkSize,
      @FormDataParam("flowFilename") String flowFilename,
      @FormDataParam("flowIdentifier") String flowIdentifier,
      @FormDataParam("flowRelativePath") String flowRelativePath,
      @FormDataParam("flowTotalChunks") String flowTotalChunks,
      @FormDataParam("flowTotalSize") String flowTotalSize,
      @Context HttpServletRequest req,
      @Context SecurityContext sc) throws DatasetException, ProjectException, AccessControlException {
    configureUploader(sc);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    FlowInfo flowInfo = new FlowInfo(flowChunkNumber, flowChunkSize, flowCurrentChunkSize, flowFilename, flowIdentifier,
      flowRelativePath, flowTotalChunks, flowTotalSize);
    validate(flowInfo);
    boolean finished = uploadController.upload(uploadedInputStream, flowInfo, this.path, this.username);

    if (finished) {
      json.setSuccessMessage("Successfuly uploaded file to " + this.path);
      return Response.status(Response.Status.OK).entity(json).build();
    }
    json.setSuccessMessage("Uploading...");
    return Response.status(Response.Status.OK).entity(json).build();
  }
}
