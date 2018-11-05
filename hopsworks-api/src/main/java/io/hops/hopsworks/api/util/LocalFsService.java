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
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.hdfs.inode.FsView;
import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.jwt.annotation.JWTRequired;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LocalFsService {

  private static final  Logger logger = Logger.getLogger(LocalFsService.class.getName());

  @EJB
  private NoCacheResponse noCacheResponse;

  private Integer projectId;

  public LocalFsService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
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
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response createDataSetDir(String path) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
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

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(json).build();
  }

  @DELETE
  @Path("/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removedataSetdir(@PathParam("fileName") String fileName) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    if (fileName == null || fileName.isEmpty()) {
      throw new IllegalArgumentException("fileName was not provided.");
    }
    File f = new File(fileName);
    boolean res = f.delete();

    if (res) {
      json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    } else {
      json.setErrorMsg(ResponseMessages.FILE_NOT_FOUND);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();

  }

  @GET
  @Path("fileExists/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response checkFileExist(@PathParam("path") String path) throws DatasetException {
    if (path == null) {
      path = "";
    }
    File f = new File(path);
    boolean exists = f.exists();

    String message = "";
    RESTApiJsonResponse response = new RESTApiJsonResponse();

    //if it exists and it's not a dir, it must be a file
    if (exists) {
      message = "FILE";
      response.setSuccessMessage(message);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();
    }
    throw new DatasetException(RESTCodes.DatasetErrorCode.INVALID_PATH_FILE, Level.FINE, "path: " + path);
  }

  @GET
  @Path("isDir/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response isDir(@PathParam("path") String path) throws DatasetException {

    if (path == null) {
      path = "";
    }

    File f = new File(path);
    boolean exists = f.exists();
    boolean isDir = f.isDirectory();

    String message = "";
    RESTApiJsonResponse response = new RESTApiJsonResponse();

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
  
    throw new DatasetException(RESTCodes.DatasetErrorCode.INVALID_PATH_DIR, Level.FINE, "path: " + path);
  }

}
