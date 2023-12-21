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

package io.hops.hopsworks.api.serving.inference;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.filter.featureFlags.FeatureFlagRequired;
import io.hops.hopsworks.api.filter.featureFlags.FeatureFlags;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.serving.inference.InferenceController;
import io.hops.hopsworks.common.serving.inference.InferenceEndpoint;
import io.hops.hopsworks.common.serving.inference.InferenceVerb;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.logging.Logger;

/**
 * RESTful microservice for sending inference requests to models being served on Hopsworks.
 * Works as a proxy, it takes in the user request and relays it to the right model.
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Model inference service", description = "Handles inference requests for ML models")
public class InferenceResource {

  @EJB
  private InferenceController inferenceController;
  @EJB
  private ProjectFacade projectFacade;

  private Project project;

  private final static Logger logger = Logger.getLogger(InferenceResource.class.getName());

  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }

  @POST
  @Path("/models/{modelName: [a-zA-Z0-9]+}{version:(/versions/[0-9]+)?}{verb:((" + InferenceVerb.ANNOTATION + "))?}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Make inference")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB, Audience.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  public Response infer(
      @ApiParam(value = "Name of the model to query", required = true) @PathParam("modelName") String modelName,
      @ApiParam(value = "Version of the model to query") @PathParam("version") String modelVersion,
      @ApiParam(value = "Type of query") @PathParam("verb") InferenceVerb verb,
      @Context SecurityContext sc,
      @Context HttpHeaders httpHeaders, String inferenceRequestJson) throws InferenceException, ApiKeyException {
    Integer version = null;
    if (!Strings.isNullOrEmpty(modelVersion)) {
      version = Integer.valueOf(modelVersion.split("/")[2]);
    }
    String authHeader = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0);
    String inferenceResult = inferenceController.infer(project, sc.getUserPrincipal().getName(), modelName, version,
      verb, inferenceRequestJson, authHeader);
    return Response.ok().entity(inferenceResult).build();
  }
  
  @GET
  @Path("/endpoints")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.SERVING},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  @ApiOperation(value = "Get inference endpoints for model serving", response = InferenceEndpoint.class,
    responseContainer = "List")
  public Response getEndpoints(@Context SecurityContext sc, @Context HttpServletRequest req) throws ServingException {
    List<InferenceEndpoint> endpoints = inferenceController.getInferenceEndpoints();
    GenericEntity<List<InferenceEndpoint>> endpointsEntity = new GenericEntity<List<InferenceEndpoint>>(endpoints){};
    return Response.ok().entity(endpointsEntity).build();
  }
}
