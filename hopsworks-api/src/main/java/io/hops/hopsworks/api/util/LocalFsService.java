/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.hdfs.inode.FsView;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import java.util.logging.Level;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LocalFsService {

  private final static Logger logger = Logger.getLogger(LocalFsService.class.
          getName());

  @EJB
  private ProjectFacade projectFacade;
//  @EJB
//  private DatasetRequestFacade datasetRequest;
  @EJB
  private ActivityFacade activityFacade;
//  @EJB
//  private UserManager userBean;
  @EJB
  private NoCacheResponse noCacheResponse;
//  @EJB
//  private FileOperations fileOps;

  @EJB
  private Settings settings;
  @Inject
  DownloadService downloader;

  private Integer projectId;
  private Project project;
  private String path;
//  private Dataset dataset;

  public LocalFsService() {
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

  private Response dirListing(String path) {
    logger.log(Level.INFO, "Dir listing for local path: {0}", path);
    File baseDir = new File(path);
    if (baseDir.exists() == false || baseDir.isDirectory() == false) {
      return noCacheResponse.getNoCacheResponseBuilder(
              Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    List<FsView> results = new ArrayList<>();
    File[] files = baseDir.listFiles();

    for (File file : files) {
      if (file.isDirectory()) {
        results.add(new FsView(file.getAbsolutePath(), true));
      } else {
        results.add(new FsView(file.getAbsolutePath(), false));
      }
    }

    GenericEntity<List<FsView>> fileViews = new GenericEntity<List<FsView>>(
            results) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            fileViews).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response createDataSetDir(
          String path,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    File f = new File(path);
    if (f.exists()) {
      json.setErrorMsg("File already exists: " + path);
    } else {
      boolean res = f.mkdir();
      if (res) {
        json.setSuccessMessage("Created directory: " + path);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(
                        json).build();
      } else {
        json.setErrorMsg("Could not create directory: " + path);
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(
            Response.Status.INTERNAL_SERVER_ERROR).entity(
                    json).build();
  }

  @DELETE
  @Path("/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response removedataSetdir(
          @PathParam("fileName") String fileName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    boolean success = false;
    JsonResponse json = new JsonResponse();
    if (fileName == null || fileName.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }
    File f = new File(fileName);
    boolean res = f.delete();

    if (res) {
      json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    } else {
      json.setErrorMsg(ResponseMessages.FILE_NOT_FOUND);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();

  }

  @GET
  @Path("fileExists/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response checkFileExist(@PathParam("path") String path) throws
          AppException {
    if (path == null) {
      path = "";
    }
    File f = new File(path);
    boolean exists = f.exists();

    String message = "";
    JsonResponse response = new JsonResponse();

    //if it exists and it's not a dir, it must be a file
    if (exists) {
      message = "FILE";
      response.setSuccessMessage(message);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();
    }
    throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            "The requested path does not resolve to a valid file");
  }

  @GET
  @Path("isDir/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response isDir(@PathParam("path") String path) throws
          AppException {

    if (path == null) {
      path = "";
    }

    File f = new File(path);
    boolean exists = f.exists();
    boolean isDir = f.isDirectory();

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

    throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            "The requested path does not resolve to a valid dir");
  }

}
