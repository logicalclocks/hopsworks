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

package io.hops.hopsworks.api.featurestore;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.hops.hopsworks.api.featurestore.datavalidationv2.greatexpectations.GreatExpectationResource;
import io.hops.hopsworks.api.featurestore.featuregroup.FeaturegroupService;
import io.hops.hopsworks.api.featurestore.featureview.FeatureViewService;
import io.hops.hopsworks.api.featurestore.storageconnector.FeaturestoreStorageConnectorService;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetService;
import io.hops.hopsworks.api.featurestore.transformationFunction.TransformationFunctionResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.kafka.KafkaResource;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreKeywordControllerIface;
import io.hops.hopsworks.common.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * A Stateless RESTful service for the featurestore service on Hopsworks.
 * Base URL: project/id/featurestores/
 */
@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Featurestore service", description = "A service that manages project's feature stores")
public class FeaturestoreService {

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private ProjectController projectController;
  @EJB
  private Settings settings;
  @Inject
  private FeaturegroupService featuregroupService;
  @Inject
  private TrainingDatasetService trainingDatasetService;
  @Inject
  private FeatureViewService featureViewService;
  @Inject
  private FeaturestoreStorageConnectorService featurestoreStorageConnectorService;
  @Inject
  private FsQueryConstructorResource fsQueryConstructorResource;
  @Inject
  private FeatureStoreKeywordControllerIface keywordCtrl;
  @EJB
  private FeaturestoreKeywordBuilder featurestoreKeywordBuilder;
  @Inject
  private TransformationFunctionResource transformationFunctionResource;
  @Inject
  private GreatExpectationResource greatExpectationResource;
  @Inject
  private KafkaResource kafkaResource;

  private Project project;

  /**
   * Set the project of the featurestore (provided by parent resource)
   *
   * @param projectId the id of the project
   */
  @Logged(logLevel = LogLevel.OFF)
  public void setProjectId(Integer projectId) throws ProjectException {
    this.project = projectController.findProjectById(projectId);
  }

  /**
   * Endpoint for getting the list of featurestores for the project
   *
   * @return list of featurestore in JSON representation
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE, ApiScope.KAFKA},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER", "AGENT"})
  @ApiOperation(value = "Get the list of feature stores for the project",
    response = FeaturestoreDTO.class,
    responseContainer = "List")
  public Response getFeaturestores(
      @Context
          HttpServletRequest req,
      @Context
          SecurityContext sc,
      @QueryParam("include_shared")
      @DefaultValue("true")
      @ApiParam(value = "include_shared=false",
          allowableValues = "include_shared=false,include_shared=true",
          defaultValue = "true")
          Boolean includeShared
  )
      throws FeaturestoreException {
    List<FeaturestoreDTO> featurestores;
    if (includeShared) {
      featurestores = featurestoreController.getFeaturestoresForProject(project);
    } else {
      featurestores = Lists.newArrayList(featurestoreController.convertFeaturestoreToDTO(
          featurestoreController.getProjectFeaturestore(project)
      ));
    }
    GenericEntity<List<FeaturestoreDTO>> featurestoresGeneric =
        new GenericEntity<List<FeaturestoreDTO>>(featurestores) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoresGeneric).build();
  }

  /**
   * Endpoint for getting a featurestore with a particular Id
   *
   * @param featurestoreId the id of the featurestore
   * @return JSON representation of the featurestore
   */
  @GET
  @Path("/{featurestoreId: [0-9]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get featurestore with specific Id",
    response = FeaturestoreDTO.class)
  public Response getFeaturestore(@ApiParam(value = "Id of the featurestore", required = true)
                                  @PathParam("featurestoreId") Integer featurestoreId,
                                  @Context HttpServletRequest req,
                                  @Context SecurityContext sc) throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreDTO featurestoreDTO = 
      featurestoreController.getFeaturestoreDTOForProjectWithId(project, featurestoreId);
    GenericEntity<FeaturestoreDTO> featurestoreDTOGeneric =
      new GenericEntity<FeaturestoreDTO>(featurestoreDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreDTOGeneric).build();
  }

  /**
   * Endpoint for getting a featurestore's settings
   **
   * @return JSON representation of the featurestore settings
   */
  @GET
  @Path("/settings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get featurestore settings",
    response = FeaturestoreClientSettingsDTO.class)
  public Response getFeaturestoreSettings(@Context HttpServletRequest req, @Context SecurityContext sc) {
    FeaturestoreClientSettingsDTO featurestoreClientSettingsDTO = new FeaturestoreClientSettingsDTO();
    featurestoreClientSettingsDTO.setOnlineFeaturestoreEnabled(settings.isOnlineFeaturestore());
    GenericEntity<FeaturestoreClientSettingsDTO> featurestoreClientSettingsDTOGeneric =
      new GenericEntity<FeaturestoreClientSettingsDTO>(featurestoreClientSettingsDTO) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreClientSettingsDTOGeneric)
      .build();
  }
  
  /**
   * Endpoint for getting a featurestore by name.
   *
   * @param name
   *   the name of the featurestore
   * @return JSON representation of the featurestore
   */
  @GET
  // Anything else that is not just number should use this endpoint
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get featurestore with a specific name",
    response = FeaturestoreDTO.class)
  public Response getFeaturestoreByName(@ApiParam(value = "Id of the featurestore", required = true)
                                        @PathParam("name") String name,
                                        @Context HttpServletRequest req,
                                        @Context SecurityContext sc) throws FeaturestoreException {
    verifyNameProvided(name);
    FeaturestoreDTO featurestoreDTO =
      featurestoreController.getFeaturestoreForProjectWithName(project, name);
    GenericEntity<FeaturestoreDTO> featurestoreDTOGeneric =
      new GenericEntity<FeaturestoreDTO>(featurestoreDTO) {
      };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(featurestoreDTOGeneric).build();
  }

  /**
   * Feature Groups sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the feature groups service
   * @throws FeaturestoreException
   */
  @Logged(logLevel = LogLevel.OFF)
  @Path("/{featurestoreId}/featuregroups")
  public FeaturegroupService featuregroupService(@PathParam("featurestoreId") Integer featurestoreId,
    @Context SecurityContext sc) throws FeaturestoreException {
    featuregroupService.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    featuregroupService.setFeaturestoreId(featurestoreId);
    return featuregroupService;
  }

  /**
   * Training Datasets sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the training dataset service
   * @throws FeaturestoreException
   */
  @Logged(logLevel = LogLevel.OFF)
  @Path("/{featurestoreId}/trainingdatasets")
  public TrainingDatasetService trainingDatasetService(@PathParam("featurestoreId") Integer featurestoreId)
      throws FeaturestoreException {
    trainingDatasetService.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    trainingDatasetService.setFeaturestoreId(featurestoreId);
    return trainingDatasetService;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("/{featurestoreId}/featureview")
  public FeatureViewService featureViewService(@PathParam("featurestoreId") Integer featurestoreId)
      throws FeaturestoreException {
    featureViewService.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    featureViewService.setFeaturestore(featurestoreId);
    return featureViewService;
  }

  /**
   * Storage Connectors sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the storage connector service
   * @throws FeaturestoreException
   */
  @Logged(logLevel = LogLevel.OFF)
  @Path("/{featurestoreId}/storageconnectors")
  public FeaturestoreStorageConnectorService storageConnectorService(
      @PathParam("featurestoreId") Integer featurestoreId) throws FeaturestoreException {
    featurestoreStorageConnectorService.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    featurestoreStorageConnectorService.setFeaturestoreId(featurestoreId);
    return featurestoreStorageConnectorService;
  }

  @Path("/query")
  @Logged(logLevel = LogLevel.OFF)
  public FsQueryConstructorResource constructQuery() {
    return fsQueryConstructorResource.setProject(project);
  }

  @Path("keywords")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Get available keywords for the featurestore", response = KeywordDTO.class)
  public Response getUsedKeywords(@Context SecurityContext sc,
                                  @Context HttpServletRequest req,
                                  @Context UriInfo uriInfo) {
    List<String> keywords = keywordCtrl.getAllKeywords();
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
    KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, resourceRequest, project, keywords);
    return Response.ok().entity(dto).build();
  }

  /**
   * Verify that the name was provided as a path param
   *
   * @param featureStoreName the feature store name to verify
   */
  private void verifyNameProvided(String featureStoreName) {
    if (Strings.isNullOrEmpty(featureStoreName)) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_NAME_NOT_PROVIDED.getMessage());
    }
  }

  /**
   * TransformationFunctions sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the feature store transformationfunction resource
   * @throws FeaturestoreException
   */
  @Logged(logLevel = LogLevel.OFF)
  @Path("/{featurestoreId}/transformationfunctions")
  public TransformationFunctionResource transformationResource(
      @PathParam("featurestoreId") Integer featurestoreId) throws FeaturestoreException {
    this.transformationFunctionResource.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    this.transformationFunctionResource.setFeaturestore(
        featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId));
    return transformationFunctionResource;
  }

  /**
   * GreatExpectations sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the feature store greatExpectation resource
   * @throws FeaturestoreException
   */
  @Logged(logLevel = LogLevel.OFF)
  @Path("/{featurestoreId}/greatexpectations")
  public GreatExpectationResource greatExpectationResource(
      @PathParam("featurestoreId") Integer featurestoreId) throws FeaturestoreException {
    this.greatExpectationResource.setProject(project);
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    this.greatExpectationResource.setFeaturestore(
        featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId));
    return greatExpectationResource;
  }

  /**
   * Kafka sub-resource
   *
   * @param featurestoreId id of the featurestore
   * @return the feature store kafka resource
   * @throws FeaturestoreException
   */
  @Logged(logLevel = LogLevel.OFF)
  @Path("{featurestoreId}/kafka")
  public KafkaResource kafkaResource(@PathParam("featurestoreId") Integer featurestoreId)
      throws FeaturestoreException {
    if (featurestoreId == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ID_NOT_PROVIDED.getMessage());
    }
    //This call verifies that the project have access to the featurestoreId provided
    Featurestore featurestore = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
    this.kafkaResource.setProject(featurestore.getProject());
    return this.kafkaResource;
  }
}
