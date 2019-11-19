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

package io.hops.hopsworks.api.serving;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.serving.util.ServingCommands;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.UnsupportedEncodingException;

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
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * RESTful microservice for model servings on Hopsworks. Supports both Tensorflow and SKLearn models.
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "TensorFlow Serving service", description = "Manage Serving instances")
public class ServingService {

  @Inject
  private ServingController servingController;
  @EJB
  private ServingUtil servingUtil;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;

  /*
    @POST
    project/id/serving/

    Serving {
      artifact_path
      name
    }

    Get @GET  project/id/serving/
    Get Single @GET project/id/serving/12
    Delete @Delete project/id/serving/12
    POST project/id/serving/12 {action: start | stop}
   */

  private Project project;

  public ServingService(){ }

  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of serving instances for the project",
      response = ServingView.class,
      responseContainer = "List")
  public Response getServings() throws ServingException, KafkaException, CryptoPasswordNotFoundException {
    List<ServingWrapper> servingDAOList = servingController.getServings(project);
    
    ArrayList<ServingView> servingViewList = new ArrayList<>();
    for (ServingWrapper servingWrapper : servingDAOList) {
      servingViewList.add(new ServingView(servingWrapper));
    }

    GenericEntity<ArrayList<ServingView>> genericListServingView =
        new GenericEntity<ArrayList<ServingView>>(servingViewList){};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(genericListServingView)
        .build();
  }

  @GET
  @Path("/{servingId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get info about a serving instance for the project", response = ServingView.class)
  public Response getServing(
      @ApiParam(value = "Id of the Serving instance", required = true) @PathParam("servingId") Integer servingId)
      throws ServingException, KafkaException, CryptoPasswordNotFoundException {
    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }
    ServingWrapper servingWrapper = servingController.getServing(project, servingId);

    ServingView servingView = new ServingView(servingWrapper);
    GenericEntity<ServingView> servingEntity = new GenericEntity<ServingView>(servingView){};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(servingEntity)
        .build();
  }

  @DELETE
  @Path("/{servingId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a serving instance")
  public Response deleteServing(
      @ApiParam(value = "Id of the serving instance", required = true) @PathParam("servingId") Integer servingId)
      throws ServingException {
    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }

    servingController.deleteServing(project, servingId);

    return Response.ok().build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create or update a serving instance")
  public Response createOrUpdate(@Context SecurityContext sc,
      @ApiParam(value = "serving specification", required = true)
        ServingView serving)
      throws ServingException, ServiceException, KafkaException, ProjectException, UserException,
        InterruptedException, ExecutionException, UnsupportedEncodingException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (serving == null) {
      throw new IllegalArgumentException("serving was not provided");
    }
    ServingWrapper servingWrapper = serving.getServingWrapper();
    servingUtil.validateUserInput(servingWrapper, project);
    servingController.createOrUpdate(project, user, servingWrapper);
    return Response.status(Response.Status.CREATED).build();
  }

  @POST
  @Path("/{servingId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Start or stop a Serving instance")
  public Response startOrStop(@Context SecurityContext sc,
      @ApiParam(value = "ID of the Serving instance to start/stop", required = true)
      @PathParam("servingId") Integer servingId,
      @ApiParam(value = "Action", required = true) @QueryParam("action") ServingCommands servingCommand)
      throws ServingException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }
  
    if (servingCommand == null) {
      throw new IllegalArgumentException(RESTCodes.ServingErrorCode.COMMANDNOTPROVIDED.getMessage());
    }
    
    servingController.startOrStop(project, user, servingId, servingCommand);

    return Response.ok().build();
  }
}
