/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.featuremonitoring.result;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.result.FeatureMonitoringResultController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.result.FeatureMonitoringResultDTO;
import io.hops.hopsworks.common.featurestore.featuremonitoring.result.FeatureMonitoringResultInputValidation;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.result.FeatureMonitoringResult;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Logged
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Feature Monitoring Result resource")
public abstract class FeatureMonitoringResultResource {
  
  @EJB
  private FeatureMonitoringResultController featureMonitoringResultController;
  @EJB
  private FeatureMonitoringResultInputValidation featureMonitoringResultInputValidation;
  @EJB
  private FeatureMonitoringResultBuilder featureMonitoringResultBuilder;
  @EJB
  private FeatureMonitoringConfigurationController featureMonitoringConfigurationController;
  
  protected Project project;
  protected Featurestore featureStore;
  
  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureStore(Featurestore featureStore) {
    this.featureStore = featureStore;
  }
  
  /**
   * Endpoint to fetch a list of feature monitoring results attached to a Feature.
   *
   * @param pagination
   * @param featureMonitoringResultBeanParam
   * @return JSON-array of feature monitoring results
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch all feature monitoring results connected to a feature monitoring configuration",
    response = FeatureMonitoringResultDTO.class, responseContainer = "List")
  @GET
  @Path("/byconfig/{configId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getAllByConfigId(@BeanParam Pagination pagination,
    @BeanParam FeatureMonitoringResultBeanParam featureMonitoringResultBeanParam,
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "Id of the configuration", required = true)
    @PathParam("configId") Integer configId
  ) throws FeaturestoreException {
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.FEATURE_MONITORING);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(featureMonitoringResultBeanParam.getSortBySet());
    resourceRequest.setFilter(featureMonitoringResultBeanParam.getFilter());
    resourceRequest.setExpansions(featureMonitoringResultBeanParam.getExpansion().getResources());
    
    AbstractFacade.CollectionInfo<FeatureMonitoringResult> results =
      featureMonitoringResultController.getAllFeatureMonitoringResultByConfigId(
        pagination.getOffset(), pagination.getLimit(), featureMonitoringResultBeanParam.getSortBySet(),
        featureMonitoringResultBeanParam.getFilter(), configId);
    
    FeatureMonitoringResultDTO dtos =
      featureMonitoringResultBuilder.buildMany(uriInfo, project, featureStore, results, resourceRequest);
    
    return Response.ok().entity(dtos).build();
  }
  
  /**
   * Endpoint to fetch a list of feature monitoring results attached to a Feature.
   *
   * @param resultId the id of the result
   * @return JSON-array of feature monitoring results
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch all feature monitoring result connected to a Feature",
    response = FeatureMonitoringResultDTO.class, responseContainer = "List")
  @GET
  @Path("/{resultId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getSingle(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "Id of the result", required = true)
    @PathParam("resultId") Integer resultId
  ) throws FeaturestoreException {
    
    FeatureMonitoringResult result = featureMonitoringResultController.getFeatureMonitoringResultById(resultId);
    FeatureMonitoringResultDTO dtos = featureMonitoringResultBuilder.buildWithStats(
      uriInfo, project, featureStore, result
    );
    
    return Response.ok().entity(dtos).build();
  }
  
  /**
   * Endpoint to persist a result connected to the monitoring of a Feature
   *
   * @param resultDTO json representation of the result generated during feature monitoring comparison.
   * @return JSON information about the created result
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Persist a result connected to the monitoring of a Feature",
    response = FeatureMonitoringResultDTO.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createFeatureMonitoringResult(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    FeatureMonitoringResultDTO resultDTO) throws FeaturestoreException {
    
    featureMonitoringResultInputValidation.validateOnCreate(resultDTO);
    FeatureMonitoringResult result = featureMonitoringResultBuilder.buildFromDTO(resultDTO);
    result.setFeatureMonitoringConfig(
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByConfigId(resultDTO.getConfigId()));
    featureMonitoringResultController.createFeatureMonitoringResult(result);
    FeatureMonitoringResultDTO dto = featureMonitoringResultBuilder.buildWithStats(
      uriInfo, project, featureStore, result
    );
    
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  /**
   * Endpoint to delete a result connected to the monitoring of a Feature
   *
   * @param resultId id of the result to delete
   * @return JSON information about the created result
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Delete a result connected to the monitoring of a Feature")
  @DELETE
  @Path("/{resultId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteFeatureMonitoringResult(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @PathParam("resultId")Integer resultId
  ) throws FeaturestoreException {
    
    featureMonitoringResultController.deleteFeatureMonitoringResult(resultId);
    
    return Response.noContent().build();
  }
}