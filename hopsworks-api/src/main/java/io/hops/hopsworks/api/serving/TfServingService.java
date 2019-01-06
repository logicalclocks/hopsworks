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

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.CryptoPasswordNotFoundException;
import io.hops.hopsworks.common.exception.KafkaException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.serving.tf.TfServingCommands;
import io.hops.hopsworks.common.serving.tf.TfServingController;
import io.hops.hopsworks.common.serving.tf.TfServingException;
import io.hops.hopsworks.common.serving.tf.TfServingModelPathValidator;
import io.hops.hopsworks.common.serving.tf.TfServingWrapper;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "TensorFlow Serving service", description = "Manage TFServing instances")
public class TfServingService {

  @Inject
  private TfServingController tfServingController;

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;

  @EJB
  private TfServingModelPathValidator tfServingModelPathValidator;

  /*
    @POST
    project/id/serving/

    TFserving {
      model_dir
      model_name
    }

    Get @GET  project/id/serving/
    Get Single @GET project/id/serving/12
    Delete @Delete project/id/serving/12
    POST project/id/serving/12 {action: start | stop}
   */

  private Project project;

  public TfServingService(){ }

  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get the list of TfServing instances for the project",
      response = TfServingView.class,
      responseContainer = "List")
  public Response getTfServings() throws TfServingException, KafkaException, CryptoPasswordNotFoundException {
    List<TfServingWrapper> servingDAOList = tfServingController.getTfServings(project);


    ArrayList<TfServingView> servingViewList = new ArrayList<>();
    for (TfServingWrapper tfServingWrapper : servingDAOList) {
      servingViewList.add(new TfServingView(tfServingWrapper));
    }

    GenericEntity<ArrayList<TfServingView>> genericListTfView =
        new GenericEntity<ArrayList<TfServingView>>(servingViewList){};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(genericListTfView)
        .build();
  }

  @GET
  @Path("/{servingId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get info about a TfServing instance for the project", response = TfServingView.class)
  public Response getTfserving(
      @ApiParam(value = "Id of the TfServing instance", required = true) @PathParam("servingId") Integer servingId)
      throws TfServingException, KafkaException, CryptoPasswordNotFoundException {
    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }
    TfServingWrapper tfServingWrapper = tfServingController.getTfServing(project, servingId);

    TfServingView tfServingView = new TfServingView(tfServingWrapper);
    GenericEntity<TfServingView> tfServingEntity = new GenericEntity<TfServingView>(tfServingView){};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(tfServingEntity)
        .build();
  }

  @DELETE
  @Path("/{servingId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Delete a TfServing instance")
  public Response deleteTfServing(
      @ApiParam(value = "Id of the TfServing instance", required = true) @PathParam("servingId") Integer servingId)
      throws TfServingException {
    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }

    tfServingController.deleteTfServing(project, servingId);

    return Response.ok().build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Create or update a TfServing instance")
  public Response createOrUpdate(@Context SecurityContext sc,
      @ApiParam(value = "TfServing specification", required = true) TfServingView tfServing)
      throws TfServingException, ServiceException, KafkaException, ProjectException, UserException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (tfServing == null) {
      throw new IllegalArgumentException("tfServing was not provided");
    }

    // Check that the modelName is present
    if (Strings.isNullOrEmpty(tfServing.getModelName())) {
      throw new IllegalArgumentException("Model name not provided");
    } else if (tfServing.getModelName().contains(" ")) {
      throw new IllegalArgumentException("Model name cannot contain spaces");
    }

    if (tfServing.getModelVersion() == null) {
      throw new IllegalArgumentException("Model version not provided");
    }

    // Check that the modelPath is present
    if (Strings.isNullOrEmpty(tfServing.getModelPath())) {
      throw new IllegalArgumentException("Model path not provided");
    } else {
      // Check that the modelPath respects the TensorFlow standard
      tfServingModelPathValidator.validateModelPath(tfServing.getModelPath(), tfServing.getModelVersion());
    }

    // Check that the batching option has been specified
    if (tfServing.isBatchingEnabled() == null) {
      throw new IllegalArgumentException("Batching is null");
    }

    // Check for duplicated entries
    tfServingController.checkDuplicates(project, tfServing.getTfServingWrapper());

    tfServingController.createOrUpdate(project, user, tfServing.getTfServingWrapper());

    return Response.status(Response.Status.CREATED).build();
  }

  @POST
  @Path("/{servingId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Start or stop a TfServing instance")
  public Response startOrStop(@Context SecurityContext sc,
      @ApiParam(value = "ID of the TfServing instance to start/stop", required = true)
      @PathParam("servingId") Integer servingId,
      @ApiParam(value = "Action", required = true) @QueryParam("action") TfServingCommands servingCommand)
      throws TfServingException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }
  
    if (servingCommand == null) {
      throw new IllegalArgumentException(RESTCodes.TfServingErrorCode.COMMANDNOTPROVIDED.getMessage());
    }

    tfServingController.startOrStop(project, user, servingId, servingCommand);

    return Response.ok().build();
  }
}