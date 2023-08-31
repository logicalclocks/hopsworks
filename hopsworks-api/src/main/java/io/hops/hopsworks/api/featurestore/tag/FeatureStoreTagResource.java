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

package io.hops.hopsworks.api.featurestore.tag;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.tags.TagsExpansionBeanParam;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.featurestore.metadata.AttachMetadataResult;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Logged
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Tags resource")
public abstract class FeatureStoreTagResource {
  
  @Inject
  protected FeatureStoreTagControllerIface tagController;
  @EJB
  protected FeatureStoreTagBuilder tagBuilder;
  
  protected Project project;
  protected Featurestore featureStore;
  
  /**
   * Set the project of the tag resource (provided by parent resource)
   *
   * @param project the project where the tag operations will be performed
   */
  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }
  
  /**
   * Sets the feature store of the tag resource
   *
   * @param featureStore
   */
  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureStore(Featurestore featureStore) {
    this.featureStore = featureStore;
  }
  
  protected abstract Optional<FeatureStoreTag> getTag(String name) throws FeatureStoreMetadataException;
  protected abstract Map<String, FeatureStoreTag> getTags();
  protected abstract AttachMetadataResult<FeatureStoreTag> putTag(String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException;
  protected abstract AttachMetadataResult<FeatureStoreTag> putTags(Map<String, String> tags)
    throws FeatureStoreMetadataException, FeaturestoreException;
  protected abstract void deleteTag(String name) throws FeatureStoreMetadataException, FeaturestoreException;
  protected abstract void deleteTags() throws FeaturestoreException;
  protected abstract TagsDTO buildPutTags(UriInfo uriInfo, ResourceRequest request, Map<String, FeatureStoreTag> tags)
    throws FeatureStoreMetadataException;
  protected abstract TagsDTO buildGetTags(UriInfo uriInfo, ResourceRequest request, Map<String, FeatureStoreTag> tags)
    throws FeatureStoreMetadataException;
  
  @ApiOperation(value = "Create or update one tag", response = TagsDTO.class)
  @PUT
  @Path("/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response putTag(@Context SecurityContext sc,
                         @Context HttpServletRequest req,
                         @Context UriInfo uriInfo,
                         @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name,
                         @ApiParam(value = "Value to set for the tag") String value)
    throws FeatureStoreMetadataException, FeaturestoreException {
    AttachMetadataResult<FeatureStoreTag> result = putTag(name, value);
    TagsDTO dto = buildPutTags(uriInfo, new ResourceRequest(ResourceRequest.Name.TAGS), result.getItems());
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @ApiOperation( value = "Create or update tags(bulk)", response = TagsDTO.class)
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response putTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          TagsDTO tagsDTO)
    throws FeatureStoreMetadataException, FeaturestoreException {
  
    Map<String, String> tags;
    if(tagsDTO.getItems() == null || tagsDTO.getItems().isEmpty()) {
      tags = new HashMap<>();
      tags.put(tagsDTO.getName(), tagsDTO.getValue());
    } else {
      tags = tagsDTO.getItems().stream().collect(Collectors.toMap(TagsDTO::getName, TagsDTO::getValue));
    }
    AttachMetadataResult<FeatureStoreTag> result = putTags(tags);
    TagsDTO dto = buildPutTags(uriInfo, new ResourceRequest(ResourceRequest.Name.TAGS), result.getItems());
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @ApiOperation( value = "Get all tags attached", response = TagsDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws FeatureStoreMetadataException {
    Map<String, FeatureStoreTag> result = getTags();
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = buildGetTags(uriInfo, resourceRequest, result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }
  
  @ApiOperation( value = "Get tag attached", response = TagsDTO.class)
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTag(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name,
                         @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws FeatureStoreMetadataException {
    Optional<FeatureStoreTag> result = getTag(name);
    if(result.isPresent()) {
      ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
      resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
      Map<String, FeatureStoreTag> resultAux = new HashMap<>();
      resultAux.put(result.get().getSchema().getName(), result.get());
      //TODO - this should return single element. Doing this for backwards compatibility
      TagsDTO dto = buildGetTags(uriInfo, resourceRequest, resultAux);
      return Response.status(Response.Status.OK).entity(dto).build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
  
  @ApiOperation( value = "Delete all attached tags")
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteTags(@Context SecurityContext sc,
                             @Context HttpServletRequest req)
    throws FeaturestoreException {
    deleteTags();
    return Response.noContent().build();
  }
  
  @ApiOperation( value = "Delete tag attached")
  @DELETE
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteTag(@Context SecurityContext sc,
                            @Context HttpServletRequest req,
                            @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name)
    throws FeaturestoreException, FeatureStoreMetadataException {
    deleteTag(name);
    return Response.noContent().build();
  }
}
