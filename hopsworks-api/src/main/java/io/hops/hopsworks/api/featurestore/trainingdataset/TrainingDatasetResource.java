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
package io.hops.hopsworks.api.featurestore.trainingdataset;

import io.hops.hopsworks.api.featurestore.statistics.StatisticsResource;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiParam;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TrainingDatasetResource {

  @Inject
  private StatisticsResource statisticsResource;

  @POST
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response create(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      TrainingDatasetDTO trainingDatasetDTO)
      throws FeaturestoreException {
    // This endpoint is used by both crateTrainingDataset and reproduceTrainingDataset.
    // It should check if dataset of a given version has existed before launching a job.
    return Response.ok().entity(new TrainingDatasetDTO()).build();
  }

  @GET
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req
  ) {
    return Response.ok().entity(new TrainingDatasetDTO()).build();

  }

  @GET
  @Path("/{version: [0-9]+}")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByVersion(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "training dataset version")
      @PathParam("version")
          Integer version
  ) {
    return Response.ok().entity(new TrainingDatasetDTO()).build();

  }

  @DELETE
  @Path("/{version: [0-9]+}")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "training dataset version")
      @PathParam("version")
          Integer version
  ) {
    return Response.ok().entity(new TrainingDatasetDTO()).build();

  }

  //Pagination, StatisticsBeanParam
  @GET
  @Path("/{version: [0-9]+}/statistics")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public StatisticsResource getStatistics(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "training dataset version")
      @PathParam("version")
          Integer version
  ) {
    return statisticsResource;
  }

  public void setFeatureView(String name, Integer version) {

  }
}
