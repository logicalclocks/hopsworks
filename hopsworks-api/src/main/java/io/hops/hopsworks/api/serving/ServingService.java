/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.featureFlags.FeatureFlagRequired;
import io.hops.hopsworks.api.filter.featureFlags.FeatureFlags;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.ProjectSubResource;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.security.QuotaEnforcementException;
import io.hops.hopsworks.common.security.QuotasEnforcement;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingLogs;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.serving.util.ServingCommands;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * RESTful microservice for model servings on Hopsworks. Supports both Tensorflow and SKLearn models.
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "TensorFlow Serving service", description = "Manage Serving instances")
public class ServingService extends ProjectSubResource {

  @Inject
  private ServingController servingController;
  @EJB
  private ServingUtil servingUtil;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ProjectController projectController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private QuotasEnforcement quotasEnforcement;


  public ServingService(){ }

  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  @ApiOperation(value = "Get the list of serving instances for the project",
      response = ServingView.class,
      responseContainer = "List")
  public Response getAll(
      @QueryParam("model") String modelName,
      @QueryParam("modelVersion") Integer modelVersion,
      @QueryParam("status") ServingStatusEnum status,
      @QueryParam("name") String servingName,
      @Context HttpServletRequest req,
      @Context SecurityContext sc)
    throws ServingException, KafkaException, CryptoPasswordNotFoundException, ProjectException {
    Project project = getProject();
    // if filter by name, return a single serving
    if (!Strings.isNullOrEmpty(servingName)) {
      ServingWrapper servingWrapper = servingController.get(project, servingName);
      if (servingWrapper == null) {
        throw new ServingException(RESTCodes.ServingErrorCode.INSTANCE_NOT_FOUND, Level.FINE);
      }

      ServingView servingView = new ServingView(servingWrapper);
      GenericEntity<ServingView> servingEntity = new GenericEntity<ServingView>(servingView){};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(servingEntity)
        .build();
    }

    // if filter by model version, without a model name
    if (Strings.isNullOrEmpty(modelName) && modelVersion != null) {
      throw new IllegalArgumentException("Cannot filter by model version without a model name");
    }

    List<ServingWrapper> servingDAOList = servingController.getAll(project, modelName, modelVersion, status);
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
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  @ApiOperation(value = "Get info about a serving instance for the project", response = ServingView.class)
  public Response get(
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @ApiParam(value = "Id of the Serving instance", required = true)
    @PathParam("servingId") Integer servingId)
    throws ServingException, KafkaException, CryptoPasswordNotFoundException, ProjectException {

    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }
    ServingWrapper servingWrapper = servingController.get(getProject(), servingId);
    if (servingWrapper == null) {
      throw new ServingException(RESTCodes.ServingErrorCode.INSTANCE_NOT_FOUND, Level.FINE);
    }

    ServingView servingView = new ServingView(servingWrapper);
    GenericEntity<ServingView> servingEntity = new GenericEntity<ServingView>(servingView){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(servingEntity)
      .build();
  }
  
  @DELETE
  @Path("/{servingId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  @ApiOperation(value = "Delete a serving instance")
  public Response delete(
    @Context HttpServletRequest req,
    @Context SecurityContext sc,
    @ApiParam(value = "Id of the serving instance", required = true) @PathParam("servingId") Integer servingId)
    throws ServingException, ProjectException {

    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }
    servingController.delete(getProject(), servingId);
    return Response.ok().build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  @ApiOperation(value = "Create or update a serving instance")
  public Response put(
    @Context HttpServletRequest req,
    @Context SecurityContext sc,
    @Context UriInfo uriInfo,
    @ApiParam(value = "serving specification", required = true) ServingView serving)
      throws ServingException, ServiceException, KafkaException, ProjectException, UserException,
      InterruptedException, ExecutionException, UnsupportedEncodingException {

    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    if (serving == null) {
      throw new IllegalArgumentException("serving was not provided");
    }
    ServingWrapper servingWrapper = serving.getServingWrapper();
    if (serving.getId() == null) {
      // Creating a new deployment, go through quotas enforcement
      try {
        quotasEnforcement.enforceModelDeploymentsQuota(project);
      } catch (QuotaEnforcementException ex) {
        throw new ServingException(RESTCodes.ServingErrorCode.CREATE_ERROR, Level.SEVERE,
                ex.getMessage(), ex.getMessage());
      }
    }
    servingUtil.validateUserInput(servingWrapper, project);
    servingController.put(project, user, servingWrapper);

    ServingView servingView = new ServingView(servingWrapper);
    UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(String.valueOf(servingView.getId()));
    GenericEntity<ServingView> servingEntity = new GenericEntity<ServingView>(servingView){};

    return Response.created(builder.build()).entity(servingEntity).build();
  }

  @POST
  @Path("/{servingId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  @ApiOperation(value = "Start or stop a Serving instance")
  public Response startOrStop(
    @Context SecurityContext sc,
    @ApiParam(value = "ID of the Serving instance to start/stop", required = true)
      @PathParam("servingId") Integer servingId,
    @ApiParam(value = "Action", required = true) @QueryParam("action") ServingCommands servingCommand)
    throws ServingException, ProjectException {

    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }

    if (servingCommand == null) {
      throw new IllegalArgumentException(RESTCodes.ServingErrorCode.COMMAND_NOT_PROVIDED.getMessage());
    }

    if (servingCommand.equals(ServingCommands.START)) {
      try {
        quotasEnforcement.enforceRunningModelDeploymentsQuota(project);
      } catch (QuotaEnforcementException ex) {
        throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLE_ERROR, Level.SEVERE,
                ex.getMessage(), ex.getMessage());
      }
    }

    servingController.startOrStop(project, user, servingId, servingCommand);

    return Response.ok().build();
  }


  @GET
  @Path("/{servingId}/logs")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  @ApiOperation(value = "Get logs of a serving instance of the project", response = ServingLogs.class,
    responseContainer = "List")
  public Response get(
    @DefaultValue("predictor") @QueryParam("component") String component,
    @DefaultValue("5") @QueryParam("tail") Integer tailingLines,
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @ApiParam(value = "Id of the Serving instance", required = true)
    @PathParam("servingId") Integer servingId)
    throws ServingException, KafkaException, CryptoPasswordNotFoundException, ProjectException {

    if (servingId == null) {
      throw new IllegalArgumentException("servingId was not provided");
    }
    if (!component.equals("predictor") && !component.equals("transformer")) {
      throw new IllegalArgumentException("component not valid, possible values are predictor or transformer");
    }

    List<ServingLogs> logs = servingController.getLogs(getProject(), servingId, component, tailingLines);
    GenericEntity<List<ServingLogs>> logsEntity = new GenericEntity<List<ServingLogs>>(logs){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
      .entity(logsEntity)
      .build();
  }
}
