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
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiScope;
import io.hops.hopsworks.common.serving.inference.InferenceController;
import io.hops.hopsworks.exceptions.InferenceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
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
  @Path("/models/{modelName: [a-zA-Z0-9]+}{version:(/versions/[0-9]+)?}{verb:((:predict|:classify|:regress))?}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Make inference")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.INFERENCE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response infer(
      @ApiParam(value = "Name of the model to query", required = true) @PathParam("modelName") String modelName,
      @ApiParam(value = "Version of the model to query") @PathParam("version") String modelVersion,
      @ApiParam(value = "Type of query") @PathParam("verb") String verb, @Context SecurityContext sc,
      String inferenceRequestJson) throws InferenceException {
    Integer version = null;
    if (!Strings.isNullOrEmpty(modelVersion)) {
      version = Integer.valueOf(modelVersion.split("/")[2]);
    }

    String inferenceResult = inferenceController.infer(project, modelName, version, verb, inferenceRequestJson);
    return Response.ok().entity(inferenceResult).build();
  }
}
