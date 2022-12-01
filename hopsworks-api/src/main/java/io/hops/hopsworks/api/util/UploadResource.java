/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensearch.common.Strings;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged(logLevel = LogLevel.FINEST)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UploadResource {
  private static final Logger LOGGER = Logger.getLogger(UploadResource.class.getName());
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private DatasetController datasetController;
  @EJB
  private InodeFacade inodes;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  
  private String path;
  private DatasetType datasetType;
  
  private Integer projectId;
  private String projectName;
  
  public UploadResource() {
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setParams(Integer projectId, String path, DatasetType datasetType) {
    this.projectId = projectId;
    this.path = path;
    this.datasetType = datasetType;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setParams(String projectName, String path, DatasetType datasetType) {
    this.projectName = projectName;
    this.path = path;
    this.datasetType = datasetType;
  }
  
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
  
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.DATASET_CREATE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response uploadStream(@FormDataParam("file") InputStream inputStream, @FormDataParam("fileName")
    String fileName, @FormDataParam("fileSize") String fileSize, @Context HttpServletRequest req,
    @Context SecurityContext sc, @Context UriInfo uriInfo) throws ProjectException, DatasetException {
    long startTime = System.currentTimeMillis();
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = this.getProject();
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, this.datasetType);
    this.path = datasetPath.getFullPath().toString();
    long copied;
    DistributedFileSystemOps dfsOps = null;
    FSDataOutputStream out = null;
    long size = Strings.isNullOrEmpty(fileSize) ? 0 : Long.parseLong(fileSize);
    try {
      Path location = new Path(this.path, fileName);
      dfsOps = dfs.getDfsOps(username);
    
      out = dfsOps.create(location);
      // copy returns int: -1 if size > 2147483647L
      if (size <= 2000000000L) {
        copied = IOUtils.copy(inputStream, out);
      } else {
        copied = IOUtils.copyLarge(inputStream, out);
      }
    } catch (AccessControlException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_ACCESS_PERMISSION_DENIED, Level.FINE,
        "Permission denied: You can not upload to this folder.", ex.getMessage());
    } catch (IOException e) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_ERROR, Level.FINE, "Failed to upload.",
        e.getMessage());
    } finally {
      IOUtils.closeQuietly(inputStream);
      IOUtils.closeQuietly(out);
      if (dfsOps != null) {
        dfs.closeDfsClient(dfsOps);
      }
    }
    long endTime = System.currentTimeMillis();
    String msg = String.format("Done uploading file with size=%dB, to %s. Took %f sec to copy.", copied,
      this.path, (float) (endTime - startTime) / 1000);
    json.setSuccessMessage(msg);
    LOGGER.log(Level.INFO, msg);
    URI href = uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(datasetPath.getAccessProject().getId().toString())
      .path(ResourceRequest.Name.DATASET.toString())
      .path(datasetPath.getRelativePath().toString())
      .build();
    return Response.created(href).entity(json).build();
  }
}
