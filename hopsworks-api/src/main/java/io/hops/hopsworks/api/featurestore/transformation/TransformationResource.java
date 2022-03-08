/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.transformation;

import io.hops.hopsworks.api.featurestore.statistics.StatisticsResource;
import io.hops.hopsworks.api.featurestore.transformationFunction.TransformationFunctionBuilder;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionAttachedDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Transformation Resource")
public class TransformationResource {

  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private TransformationFunctionBuilder transformationFunctionBuilder;
  @EJB
  private JWTHelper jWTHelper;
  @Inject
  private StatisticsResource statisticsResource;
  private Project project;
  private Featurestore featurestore;
  private Integer trainingDatasetId;

  // Copy from traindatasetService
  @GET
  @Path("/functions")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Get Transformation functions.", response = TransformationFunctionAttachedDTO.class)
  public Response getTransformationFunction(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo
  )
      throws FeaturestoreException {
    if (trainingDatasetId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_ID_NOT_PROVIDED.getMessage());
    }

    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TRANSFORMATIONFUNCTIONS);
    TransformationFunctionAttachedDTO transformationFunctionAttachedDTO =
        transformationFunctionBuilder.build(uriInfo, resourceRequest, user, project, trainingDataset);
    return Response.ok().entity(transformationFunctionAttachedDTO).build();
  }

  @Path("/statistics")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public StatisticsResource statistics(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    return statisticsResource;
  }
}
