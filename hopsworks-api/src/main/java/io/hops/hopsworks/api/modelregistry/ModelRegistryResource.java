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

package io.hops.hopsworks.api.modelregistry;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.filter.featureFlags.FeatureFlagRequired;
import io.hops.hopsworks.api.filter.featureFlags.FeatureFlags;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.modelregistry.dto.ModelRegistryDTO;
import io.hops.hopsworks.api.modelregistry.models.ModelsController;
import io.hops.hopsworks.api.modelregistry.models.ModelsResource;
import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelRegistryResource {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ModelRegistryBuilder modelRegistryBuilder;
  @EJB
  private ModelsController modelsController;
  @Inject
  private ModelsResource modelsResource;
  @EJB
  private JWTHelper jwtHelper;

  private Project project;
  public ModelRegistryResource setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }

  @ApiOperation(value = "Get a list of all model registries accessible for this project",
          response = ModelRegistryDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.MODELREGISTRY},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  public Response getAll(
    @BeanParam ModelRegistryBeanParam modelRegistryBeanParam,
    @BeanParam Pagination pagination,
    @Context UriInfo uriInfo,
    @Context SecurityContext sc) throws GenericException, ModelRegistryException, FeatureStoreMetadataException,
          MetadataException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELREGISTRIES);
    resourceRequest.setExpansions(modelRegistryBeanParam.getExpansions().getResources());
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    ModelRegistryDTO dto = modelRegistryBuilder.build(uriInfo, resourceRequest, user, project);

    return Response.ok().entity(dto).build();
  }

  @ApiOperation( value = "Get a model registry", response = ModelDTO.class)
  @GET
  @Path("{modelRegistryId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.MODELREGISTRY},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @FeatureFlagRequired(requiredFeatureFlags = {FeatureFlags.DATA_SCIENCE_PROFILE})
  public Response get (
          @PathParam("modelRegistryId") Integer modelRegistryId,
          @BeanParam ModelRegistryBeanParam modelRegistryBeanParam,
          @BeanParam Pagination pagination,
          @Context UriInfo uriInfo,
          @Context SecurityContext sc) throws GenericException, ModelRegistryException, FeatureStoreMetadataException,
          MetadataException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELREGISTRIES);
    resourceRequest.setExpansions(modelRegistryBeanParam.getExpansions().getResources());
    Project modelRegistryProject =
            modelsController.verifyModelRegistryAccess(project, modelRegistryId).getParentProject();
    ModelRegistryDTO dto = modelRegistryBuilder.build(uriInfo, resourceRequest, user, project, modelRegistryProject);
    return Response.ok().entity(dto).build();
  }

  /**
   * Models sub-resource
   *
   * @param modelRegistryId id of the model registry
   * @return the models resource
   * @throws ModelRegistryException
   */
  @Path("/{modelRegistryId}/models")
  public ModelsResource modelsResource(@PathParam("modelRegistryId") Integer modelRegistryId,
                                                 @Context SecurityContext sc) throws ModelRegistryException {
    modelsResource.setProject(project);
    if (modelRegistryId == null) {
      throw new IllegalArgumentException(RESTCodes.ModelRegistryErrorCode.MODEL_REGISTRY_ID_NOT_PROVIDED.getMessage());
    }
    modelsResource.setModelRegistryId(modelRegistryId);
    return modelsResource;
  }
}