/*
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
 */

package io.hops.hopsworks.api.tensorflow;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.exception.TensorBoardCleanupException;
import io.hops.hopsworks.common.experiments.TensorBoardController;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.PersistenceException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.NotFoundException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TensorBoardService {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private InodeFacade inodesFacade;
  @EJB
  private TensorBoardController tensorBoardController;
  @EJB
  private ElasticController elasticController;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private Settings settings;
  @EJB
  private NoCacheResponse noCacheResponse;

  private Project project;

  public TensorBoardService(){
  }

  public void setProjectId(Integer projectId) {
    this.project = this.projectFacade.find(projectId);
  }

  public Project getProject() {
    return project;
  }

  private final static Logger LOGGER = Logger.getLogger(TensorBoardService.class.getName());

  @ApiOperation("Get the running TensorBoard of the logged in user in this project")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getTensorBoard(@Context SecurityContext sc) throws AppException {

    try {
      Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
      TensorBoardDTO tbDTO = tensorBoardController.getTensorBoard(project, user);
      if(tbDTO == null) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
      }
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(tbDTO).build();
    } catch (PersistenceException pe) {
      LOGGER.log(Level.SEVERE, "Failed to fetch TensorBoard from database", pe);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "Could not get the running TensorBoard.");
    }
  }

  @ApiOperation("Start a new TensorBoard for the logged in user")
  @POST
  @Path("/{elasticId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response startTensorBoard(@PathParam("elasticId") String elasticId,
                                            @Context SecurityContext sc) throws AppException {

    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);

    String hdfsLogdir = null;
    try {
      hdfsLogdir = elasticController.getLogdirFromElastic(project, elasticId);
      hdfsLogdir = tensorBoardController.replaceNN(hdfsLogdir);
    } catch (NotFoundException nfe) {
      LOGGER.log(Level.SEVERE, "Could not locate logdir from elastic ", nfe);
      return null;
    }

    try {
      DsPath tbPath = pathValidator.validatePath(this.project, hdfsLogdir);
      tbPath.validatePathExists(inodesFacade, true);
    } catch(Exception e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "Experiment directory is missing, check in your project if it was deleted?.");
    }

    TensorBoardDTO tensorBoardDTO = null;
    try {
      tensorBoardDTO = tensorBoardController.startTensorBoard(elasticId, this.project, user, hdfsLogdir);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(tensorBoardDTO).build();
    } catch(TensorBoardCleanupException tbce) {
      LOGGER.log(Level.SEVERE, "Failed to start TensorBoard", tbce);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "Could not start TensorBoard.");
    }
  }

  @ApiOperation("Stop the running TensorBoard for the logged in user")
  @DELETE
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response stopTensorBoard(@Context SecurityContext sc) throws AppException {

    String loggedinemail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(loggedinemail);

    try {
      TensorBoardDTO tbDTO = tensorBoardController.getTensorBoard(project, user);
      if(tbDTO == null) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
      }

      tensorBoardController.cleanup(this.project, user);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    } catch(TensorBoardCleanupException tbce) {
      LOGGER.log(Level.SEVERE, "Failed to stop TensorBoard", tbce);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "Could not stop TensorBoard.");
    }
  }
}
