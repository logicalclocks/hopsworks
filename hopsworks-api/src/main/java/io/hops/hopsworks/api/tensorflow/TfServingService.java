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

package io.hops.hopsworks.api.tensorflow;

import io.hops.hopsworks.api.filter.NoCacheResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;


import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;

import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;

import io.hops.hopsworks.common.dao.tfserving.TfServing;
import io.hops.hopsworks.common.dao.tfserving.TfServingFacade;
import io.hops.hopsworks.common.dao.tfserving.TfServingStatusEnum;
import io.hops.hopsworks.common.dao.tfserving.config.TfServingDTO;
import io.hops.hopsworks.common.dao.tfserving.config.TfServingProcessMgr;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;

import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import org.apache.commons.codec.digest.DigestUtils;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TfServingService {
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private TfServingFacade tfServingFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private TfServingProcessMgr TfServingProcessMgr;
  @EJB
  private InodeFacade inodes;



  private Integer projectId;
  private Project project;

  public TfServingService(){

  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  private final static Logger LOGGER = Logger.getLogger(TfServingService.class.getName());


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAllTfServings(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    List<TfServing> tfServingCollection = tfServingFacade.findForProject(project);
    GenericEntity<List<TfServing>> tfServingList
            = new GenericEntity<List<TfServing>>(tfServingCollection) { };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(tfServingList).build();
  }

  @GET
  @Path("/logs/{servingId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getLogs(@PathParam("servingId") int servingId,
                         @Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    String hdfsUser = getHdfsUser(sc);
    if (hdfsUser == null) {
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Could not find your username. Report a bug.");
    }

    HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);
    if(user == null) {
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Possible inconsistency - could not find your user.");
    }

    HdfsUsers servingHdfsUser = hdfsUsersFacade.findByName(hdfsUser);
    if(!hdfsUser.equals(servingHdfsUser.getName())) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Attempting to start a serving not created by current user");
    }

    TfServing tfServing= tfServingFacade.findById(servingId);
    if (!tfServing.getProject().equals(project)) {
      return noCacheResponse.
              getNoCacheResponseBuilder(Response.Status.FORBIDDEN).build();
    }

    String logString = TfServingProcessMgr.getLogs(tfServing);

    JsonObjectBuilder arrayObjectBuilder = Json.createObjectBuilder();
    if(logString != null) {
      arrayObjectBuilder.add("stdout", logString);
    } else {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Could not get the logs for serving");
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(arrayObjectBuilder.build()).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response createTfServing(TfServing tfServing,
                                        @Context SecurityContext sc,
                                        @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String hdfsUser = getHdfsUser(sc);
    if (hdfsUser == null) {
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Could not find your username. Report a bug.");
    }

    try {
      HdfsUsers user = hdfsUsersFacade.findByName(hdfsUser);

      if(user == null) {
        throw new AppException(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Possible inconsistency - could not find your user.");
      }

      String modelPath = tfServing.getHdfsModelPath();

      if(modelPath.startsWith("hdfs://")) {
        int projectsIndex = modelPath.indexOf("/Projects");
        modelPath = modelPath.substring(projectsIndex, modelPath.length());
      }

      if(!inodes.existsPath(modelPath)) {
        throw new AppException(
                Response.Status.NOT_FOUND.getStatusCode(),
                "Could not find .pb file in the path " + modelPath);
      }

      if(modelPath.equals("")) {
        throw new AppException(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Select your .pb file corresponding to the model to be served in the Models dataset.");
      }

      tfServing.setHdfsUserId(user.getId());
      
      String secret = DigestUtils.sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt()));
      tfServing.setSecret(secret);

      tfServing.setModelName(getModelName(modelPath));
      int version = -1;
      String basePath = null;
      try {
        version = getVersion(modelPath);
        basePath = getModelBasePath(modelPath);
      } catch (Exception e) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                ".pb file should be located in Models/{model_name}/{version}");
      }

      String email = sc.getUserPrincipal().getName();

      tfServing.setVersion(version);
      tfServing.setProject(project);
      tfServing.setHdfsModelPath(basePath);
      tfServing.setStatus(TfServingStatusEnum.CREATED);
      tfServing.setCreator(userFacade.findByEmail(email));
      tfServingFacade.persist(tfServing);

    } catch (DatabaseException dbe) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                dbe.getMessage());
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(tfServing).build();
  }

  @DELETE
  @Path("/{servingId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response deleteTfServing(@PathParam("servingId") int servingId,
                            @Context SecurityContext sc,
                            @Context HttpServletRequest req) throws AppException {

    TfServing tfServing = tfServingFacade.findById(servingId);

    try {

      if (tfServing == null) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
        //Users outside the project shouldn't be able to delete a serving
      } else if (!tfServing.getProject().equals(project)) {
        return noCacheResponse.
                getNoCacheResponseBuilder(Response.Status.FORBIDDEN).build();
        //Running serving should not be possible to shutdown
      } else if (tfServing.getStatus().equals(TfServingStatusEnum.RUNNING)) {
        return noCacheResponse.
                getNoCacheResponseBuilder(Response.Status.FORBIDDEN).build();
        //Serving is CREATED or STOPPED and safe to delete
      } else {
        tfServingFacade.remove(tfServing);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
      }
    } catch (DatabaseException ex) {
      LOGGER.log(Level.WARNING,
              "Serving could not be deleted with id: " + tfServing.getId());
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ex.getMessage());
    }

  }


  @POST
  @Path("/start/{servingId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response startTfServing(@PathParam("servingId") int servingId,
                                      @Context SecurityContext sc,
                                      @Context HttpServletRequest req) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    String hdfsUser = getHdfsUser(sc);
    if (hdfsUser == null) {
      throw new AppException(
         Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Could not find your username.");
    }

    TfServing tfServing = tfServingFacade.findById(servingId);

    if (!tfServing.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    HdfsUsers servingHdfsUser = hdfsUsersFacade.findByName(hdfsUser);

    if(!hdfsUser.equals(servingHdfsUser.getName())) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Attempting to start a serving not created by current user");
    }

    if(project.getPaymentType().equals(PaymentType.PREPAID)) {
      YarnProjectsQuota projectQuota = yarnProjectsQuotaFacade.findByProjectName(project.getName());
      if (projectQuota == null || projectQuota.getQuotaRemaining() < 0) {
        throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), "This project is out of credits.");
      }
    }

    TfServingStatusEnum status = tfServing.getStatus();
    if (status.equals(TfServingStatusEnum.CREATED) ||
      status.equals(TfServingStatusEnum.STOPPED)) {

      try {

        tfServing.setStatus(TfServingStatusEnum.STARTING);
        tfServingFacade.updateRunningState(tfServing);

        TfServingDTO tfServingDTO = TfServingProcessMgr.startTfServingAsTfServingUser(hdfsUser, tfServing);

        if(tfServingDTO.getExitValue() != 0) {

          tfServing.setStatus(status);
          tfServingFacade.updateRunningState(tfServing);

          throw new AppException(
                  Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal error - could not start serving "
                  + tfServing.getModelName()+".");
        }

        tfServing.setPid(tfServingDTO.getPid());
        tfServing.setPort(tfServingDTO.getPort());
        tfServing.setHostIp(tfServingDTO.getHostIp());
        tfServing.setStatus(TfServingStatusEnum.RUNNING);

        tfServingFacade.updateRunningState(tfServing);

      } catch (IOException | InterruptedException | DatabaseException  e) {
        LOGGER.log(Level.SEVERE, "Could not start serving " + tfServing.getModelName(), e);
        throw new AppException(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal error - could not start serving "
                + tfServing.getModelName()+".");
      }

    } else {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
                  "Attempting to start an already running serving.");
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("/transform/{servingId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response transformGraph(@PathParam("servingId") int servingId,
                                      @Context SecurityContext sc,
                                      @Context HttpServletRequest req) throws AppException {

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
  
  
  @PUT
  @Path("/version")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response changeTfServingVersion(TfServing tfServing,
                                 @Context SecurityContext sc,
                                 @Context HttpServletRequest req) throws AppException {

    String modelPath = tfServing.getHdfsModelPath();
    int servingId = tfServing.getId();

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    String hdfsUser = getHdfsUser(sc);
    if (hdfsUser == null) {
      throw new AppException(
              Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Could not find your username.");
    }

    tfServing = tfServingFacade.findById(servingId);

    if (!tfServing.getProject().equals(project)) {
      //In this case, a user is trying to access a job outside its project!!!
      LOGGER.log(Level.SEVERE,"A user is trying to start a serving outside their project!");
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    //Validate model path

    String modelName = getModelName(modelPath);

    if(!tfServing.getModelName().equals(modelName)) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              "Can only change version of the same model.");
    }

    TfServingStatusEnum status = tfServing.getStatus();
    if (status.equals(TfServingStatusEnum.CREATED) ||
            status.equals(TfServingStatusEnum.STOPPED)) {

      if(modelPath.startsWith("hdfs://")) {
        int projectsIndex = modelPath.indexOf("/Projects");
        modelPath = modelPath.substring(projectsIndex, modelPath.length());
      }

      if(!inodes.existsPath(modelPath)) {
        throw new AppException(
                Response.Status.NOT_FOUND.getStatusCode(),
                "Could not find .pb file in the path " + modelPath);
      }


      tfServing.setHdfsModelPath(getModelBasePath(modelPath));
      tfServing.setVersion(getVersion(modelPath));
      try {
        tfServingFacade.updateServingVersion(tfServing);
      } catch (DatabaseException dbe) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Unable to swap model due to database error.");
      }

    } else {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Can't change version of a model currently running");
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("/stop/{servingId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response stopTfServing(@PathParam("servingId") int servingId,
                                 @Context SecurityContext sc,
                                 @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    try {
      String hdfsUser = getHdfsUser(sc);
      if (hdfsUser == null) {
        throw new AppException(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Could not find your username.");
      }

      HdfsUsers servingHdfsUser = hdfsUsersFacade.findByName(hdfsUser);

      if(!hdfsUser.equals(servingHdfsUser.getName())) {
        throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
                "Attempting to stop a serving not started by current user");
      }

      TfServing tfServing = tfServingFacade.findById(servingId);

      if (!tfServing.getProject().equals(project)) {
        //In this case, a user is trying to access a job outside its project!!!
        LOGGER.log(Level.SEVERE,"A user is trying to create a serving outside their project!");
        return Response.status(Response.Status.FORBIDDEN).build();
      }

      if (tfServing.getStatus().equals(TfServingStatusEnum.RUNNING)) {
        int exitCode = TfServingProcessMgr.killServingAsServingUser(tfServing);

        if(exitCode == 0) {
          tfServing.setStatus(TfServingStatusEnum.STOPPED);
          tfServing.setPid(null);
          tfServing.setPort(null);
          tfServing.setHostIp(null);
          tfServingFacade.updateRunningState(tfServing);

          //removeDirectory and reset serving to normal
        } else {
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                  "Serving with id " + servingId + " could not be stopped");
        }
      } else {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Attempting to stop serving with status " + tfServing.getStatus());
      }

    } catch (DatabaseException dbe) {
      LOGGER.log(Level.WARNING, "Serving with id " + servingId + " could not be stopped");
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), dbe.getMessage());
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  private String getHdfsUser(SecurityContext sc) throws AppException {
    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);
    if (user == null) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              "You are not authorized for this invocation.");
    }
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    return hdfsUsername;
  }

  private String getModelName(String modelPath) {
    String[] modelPathSplit = modelPath.split("/");
    return modelPathSplit[modelPathSplit.length-3];
  }

  private int getVersion(String modelPath) {
    String[] modelPathSplit = modelPath.split("/");
    String versionString = modelPathSplit[modelPathSplit.length-2];
    int version = Integer.parseInt(versionString);
    return version;
  }

  private String getModelBasePath(String modelPath) {
    StringBuilder modelBasePathSB = new StringBuilder();

    String [] modelPathSplit = modelPath.split("/");

    for(int i = 0; i < modelPathSplit.length -3; i++) {
      modelBasePathSB.append(modelPathSplit[i] + "/");
    }
    return modelBasePathSB.toString();
  }

}