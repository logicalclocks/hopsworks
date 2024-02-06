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
package io.hops.hopsworks.api.featurestore.featuremonitoring.config;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.JobsBuilder;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationDTO;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationInputValidation;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Logged
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Feature Monitoring Configuration resource")
public abstract class FeatureMonitoringConfigurationResource {
  
  @EJB
  private FeatureMonitoringConfigurationController featureMonitoringConfigurationController;
  @EJB
  private FeaturegroupController featureGroupController;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private FeatureMonitoringConfigurationBuilder featureMonitoringConfigurationBuilder;
  @EJB
  private JobsBuilder jobsBuilder;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private FeatureMonitoringConfigurationInputValidation featureMonitoringConfigurationInputValidation;
  
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
  
  protected abstract Integer getItemId();
  protected abstract ResourceRequest.Name getItemType();
  
  // Added to init engine class for FV only
  protected abstract String getItemName();
  protected abstract Integer getItemVersion();
  
  /**
   * Endpoint to fetch the feature monitoring configuration with a given id.
   *
   * @param configId id of the configuration to fetch
   * @return JSON information regarding the monitoring configuration connected to a Feature
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch the feature monitoring configuration connected to a Feature",
    response = FeatureMonitoringConfigurationDTO.class)
  @GET
  @Path("/{configId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getById(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "Id of the Feature Monitoring Configuration", required = true)
    @PathParam("configId") Integer configId
  ) throws FeaturestoreException {
    
    FeatureMonitoringConfiguration config =
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByConfigId(configId);
    
    FeatureMonitoringConfigurationDTO dto =
      featureMonitoringConfigurationBuilder.build(
        uriInfo, featureStore, getItemType(), getItemId(), getItemName(), getItemVersion(), config);
    
    return Response.ok().entity(dto).build();
  }
  
  /**
   * Endpoint to fetch the feature monitoring configuration with a given name.
   *
   * @param name name of the configuration to fetch
   * @return JSON information regarding the monitoring configuration connected to a Feature
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch the feature monitoring configuration by its name connected to a Feature",
    response = FeatureMonitoringConfigurationDTO.class)
  @GET
  @Path("/name/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getByName(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "Name of the Feature Monitoring Configuration", required = true)
    @PathParam("name") String name
  ) throws FeaturestoreException {
    
    FeatureMonitoringConfiguration config =
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByEntityAndName(
        featureStore, getItemType(), getItemId(), name);
    
    FeatureMonitoringConfigurationDTO dto = featureMonitoringConfigurationBuilder.build(
      uriInfo, featureStore, getItemType(), getItemId(), getItemName(), getItemVersion(), config);
    
    
    return Response.ok().entity(dto).build();
  }
  
  /**
   * Endpoint to fetch the feature monitoring configuration attached to a Feature.
   *
   * @param featureName name of the feature to monitor
   * @return JSON information about the monitoring configuration connected to featureName
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Fetch a feature monitoring configuration connected to a Feature",
    response = FeatureMonitoringConfigurationDTO.class)
  @GET
  @Path("feature/{featureName: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getByFeatureName(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "Name of the feature", required = true)
    @PathParam("featureName")String featureName
  ) throws FeaturestoreException {
    
    List<FeatureMonitoringConfiguration> configs =
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByEntityAndFeatureName(featureStore,
        getItemType(), getItemId(), featureName);
    
    FeatureMonitoringConfigurationDTO dto =featureMonitoringConfigurationBuilder.buildMany(
      uriInfo, featureStore, getItemType(), getItemId(), getItemName(), getItemVersion(), configs);
    
    return Response.ok().entity(dto).build();
  }
  
  /**
   * Endpoint to fetch the feature monitoring configuration connected to a Feature Group or Feature View.
   *
   * @return JSON information about the monitoring configuration connected to the entity
   * @throws FeaturestoreException
   */
  @ApiOperation(
    value = "Fetch a feature monitoring configuration connected to a Feature Group or Feature View",
    response = FeatureMonitoringConfigurationDTO.class)
  @GET
  @Path("entity")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getByEntityId(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo
  ) throws FeaturestoreException {
    
    List<FeatureMonitoringConfiguration> configs =
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByEntity(
        featureStore, getItemType(), getItemId());
    
    FeatureMonitoringConfigurationDTO dto = featureMonitoringConfigurationBuilder.buildMany(
      uriInfo, featureStore, getItemType(), getItemId(), getItemName(), getItemVersion(), configs);
    
    return Response.ok().entity(dto).build();
  }
  
  /**
   * Endpoint to persist a configuration connected to the monitoring of a Feature
   *
   * @param configDTO json representation of the feature monitoring configuration
   * @return JSON information about the created configuration
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Persist the configuration connected to the monitoring of a Feature",
    response = FeatureMonitoringConfigurationDTO.class)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createFeatureMonitoringConfiguration(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    FeatureMonitoringConfigurationDTO configDTO
  ) throws FeaturestoreException, JobException {
    Users user = jWTHelper.getUserPrincipal(sc);
    
    Featuregroup featureGroup = null;
    FeatureView featureView = null;
    if (getItemType() == ResourceRequest.Name.FEATUREGROUPS) {
      featureGroup = featureGroupController.getFeaturegroupById(featureStore, getItemId());
    } else if (getItemType()  == ResourceRequest.Name.FEATUREVIEW) {
      featureView = featureViewController.getByIdAndFeatureStore(getItemId(), featureStore);
    }
    featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, configDTO, featureGroup, featureView);
    FeatureMonitoringConfiguration config =
      featureMonitoringConfigurationBuilder.buildFromDTO(project, featureGroup, featureView, configDTO);
    config = featureMonitoringConfigurationController.createFeatureMonitoringConfiguration(
      featureStore, user, getItemType(), config);
    
    FeatureMonitoringConfigurationDTO dto = featureMonitoringConfigurationBuilder.build(
      uriInfo, featureStore, getItemType(), getItemId(), getItemName(), getItemVersion(), config);
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  /**
   * Endpoint to edit a configuration connected to the monitoring of a Feature
   *
   * @param configId id of the feature monitoring configuration to edit
   * @param configDTO json representation of the feature monitoring configuration to edit
   * @return JSON information about the edited configuration
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Edit the configuration connected to the monitoring of a Feature",
    response = FeatureMonitoringConfigurationDTO.class)
  @PUT
  @Path("/{configId : [0-9]+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response updateFeatureMonitoringConfiguration(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @PathParam("configId") Integer configId,
    FeatureMonitoringConfigurationDTO configDTO
  ) throws FeaturestoreException, JobException {
    
    Featuregroup featureGroup = null;
    FeatureView featureView = null;
    if (getItemType() == ResourceRequest.Name.FEATUREGROUPS) {
      featureGroup = featureGroupController.getFeaturegroupById(featureStore, getItemId());
    } else if (getItemType()  == ResourceRequest.Name.FEATUREVIEW) {
      featureView = featureViewController.getByIdAndFeatureStore(getItemId(), featureStore);
    }
    featureMonitoringConfigurationInputValidation.validateConfigOnUpdate(configDTO, featureGroup, featureView);
    FeatureMonitoringConfiguration config =
      featureMonitoringConfigurationBuilder.buildFromDTO(project, featureGroup, featureView, configDTO);
    config = featureMonitoringConfigurationController.updateFeatureMonitoringConfiguration(configId, config);
    
    FeatureMonitoringConfigurationDTO dto = featureMonitoringConfigurationBuilder.build(
      uriInfo, featureStore, getItemType(), getItemId(), getItemName(), getItemVersion(), config);
    return Response.ok(dto.getHref()).entity(dto).build();
  }
  
  /**
   * Endpoint to delete a config connected to the monitoring of a Feature
   *
   * @param configId id of the configuration to delete
   * @return Response with 204 status to indicate config has been deleted
   * @throws FeaturestoreException
   */
  @ApiOperation(value = "Delete a configuration connected to the monitoring of a Feature")
  @DELETE
  @Path("/{configId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteFeatureMonitoringConfiguration(@Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @PathParam("configId") Integer configId
  ) throws FeaturestoreException {
    
    featureMonitoringConfigurationController.deleteFeatureMonitoringConfiguration(configId);
    
    return Response.noContent().build();
  }
}