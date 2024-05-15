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

package io.hops.hopsworks.api.featurestore.query;

import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.featurestore.featureview.FeatureViewSubResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.query.FsQueryDTO;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryBuilder;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class QueryResource extends FeatureViewSubResource {

  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private QueryController queryController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private QueryBuilder queryBuilder;
  @EJB
  private ProjectController projectController;
  @EJB
  private FeaturestoreController featurestoreController;

  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }
  @Override
  protected FeaturestoreController getFeaturestoreController() {
    return featurestoreController;
  }
  @Override
  protected FeatureViewController getFeatureViewController() {
    return featureViewController;
  }

  @ApiOperation(value = "Return batch query with given event time.",
      response = FsQueryDTO.class)
  @GET
  @Path("/batch")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response constructBatchQuery(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @Context
          UriInfo uriInfo,
      @ApiParam(value = "Event start time")
      @QueryParam("start_time")
          Long startTime,
      @ApiParam(value = "Event end time")
      @QueryParam("end_time")
          Long endTime,
      @ApiParam(value = "Get query with label features")
      @QueryParam("with_label")
      @DefaultValue("false")
          Boolean withLabel,
      @ApiParam(value = "Get query with primary key features")
      @QueryParam("with_primary_keys")
      @DefaultValue("false")
        Boolean withPrimaryKeys,
      @ApiParam(value = "Get query with primary event time feature")
      @QueryParam("with_event_time")
      @DefaultValue("false")
        Boolean withEventTime,
      @ApiParam(value = "Get query with inference helper columns")
      @QueryParam("inference_helper_columns")
      @DefaultValue("false")
          Boolean inferenceHelperColumns,
      @ApiParam(value = "Get query with training helper columns")
      @QueryParam("training_helper_columns")
      @DefaultValue("false")
        Boolean trainingHelperColumns,
      @ApiParam(value = "Get query in hive format")
      @QueryParam("is_hive_engine")
      @DefaultValue("false")
          Boolean isHiveEngine,
      @ApiParam(value = "Training data version")
      @QueryParam("td_version")
          Integer trainingDataVersion
  ) throws FeaturestoreException, ServiceException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    Query query = queryController.constructBatchQuery(
        getFeatureView(featurestore), project, user, startTime, endTime, withLabel, withPrimaryKeys, withEventTime,
        inferenceHelperColumns, trainingHelperColumns, isHiveEngine, trainingDataVersion);
    return Response.ok().entity(queryBuilder.build(query, featurestore, project, user)).build();
  }

  @ApiOperation(value = "Return query originally used to create the feature view without event time filter.",
      response = QueryDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getQuery(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req
  ) throws FeaturestoreException, ServiceException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    Featurestore featurestore = getFeaturestore(project);
    Query query =
      queryController.makeQuery(getFeatureView(featurestore), project, user, true, false, false, true, true, false);
    QueryDTO queryDTO = queryBuilder.build(query, featurestore, project, user);
    return Response.ok().entity(queryDTO).build();
  }
}
