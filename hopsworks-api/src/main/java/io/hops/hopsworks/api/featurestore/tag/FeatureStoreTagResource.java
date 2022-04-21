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
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.tags.TagBuilder;
import io.hops.hopsworks.api.tags.TagsExpansionBeanParam;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.common.tags.TagControllerIface;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
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
import java.util.Map;
import java.util.stream.Collectors;

@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Tags resource")
public abstract class FeatureStoreTagResource {
  
  @Inject
  private TagControllerIface tagController;
  @EJB
  private TagBuilder tagBuilder;
  @EJB
  private JWTHelper jwtHelper;
  
  protected Project project;
  protected Featurestore featureStore;
  
  /**
   * Set the project of the tag resource (provided by parent resource)
   *
   * @param project the project where the tag operations will be performed
   */
  public void setProject(Project project) {
    this.project = project;
  }
  
  /**
   * Sets the feature store of the tag resource
   *
   * @param featureStore
   */
  public void setFeatureStore(Featurestore featureStore) {
    this.featureStore = featureStore;
  }
  
  protected abstract DatasetPath getDatasetPath() throws DatasetException;
  protected abstract Integer getItemId();
  protected abstract ResourceRequest.Name getItemType();
  
  @ApiOperation(value = "Create or update one tag", response = TagsDTO.class)
  @PUT
  @Path("/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putTag(@Context SecurityContext sc,
                         @Context HttpServletRequest req,
                         @Context UriInfo uriInfo,
                         @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name,
                         @ApiParam(value = "Value to set for the tag") String value)
    throws MetadataException, SchematizedTagException, DatasetException {
    
    Users user = jwtHelper.getUserPrincipal(sc);
    AttachTagResult result = tagController.upsert(user, getDatasetPath(), name, value);
    FeatureStoreTagUri tagUri = new FeatureStoreTagUri(uriInfo, featureStore.getId(), getItemType(), getItemId());
    TagsDTO dto = tagBuilder.build(tagUri, getDatasetPath(), result.getItems());
    
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
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response bulkPutTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                              @Context HttpServletRequest req,
                              TagsDTO tags)
    throws MetadataException, SchematizedTagException, DatasetException {
    
    Users user = jwtHelper.getUserPrincipal(sc);
    AttachTagResult result;
    
    if(tags.getItems().size() == 0) {
      result = tagController.upsert(user, getDatasetPath(), tags.getName(), tags.getValue());
    } else {
      result = tagController.upsertAll(user, getDatasetPath(), tagsToMap(tags));
    }
    FeatureStoreTagUri tagUri = new FeatureStoreTagUri(uriInfo, featureStore.getId(), getItemType(), getItemId());
    TagsDTO dto = tagBuilder.build(tagUri, getDatasetPath(), result.getItems());
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  private Map<String, String> tagsToMap(TagsDTO tags) {
    return tags.getItems().stream().collect(Collectors.toMap(TagsDTO::getName, TagsDTO::getValue));
  }
  
  @ApiOperation( value = "Get all tags attached", response = TagsDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws DatasetException, MetadataException, SchematizedTagException {
    
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    FeatureStoreTagUri tagUri = new FeatureStoreTagUri(uriInfo, featureStore.getId(), getItemType(), getItemId());
    TagsDTO dto = tagBuilder.build(tagUri, resourceRequest, user, getDatasetPath());
    return Response.status(Response.Status.OK).entity(dto).build();
  }
  
  @ApiOperation( value = "Get tag attached", response = TagsDTO.class)
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTag(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name,
                         @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws DatasetException, MetadataException, SchematizedTagException {
    
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    FeatureStoreTagUri tagUri = new FeatureStoreTagUri(uriInfo, featureStore.getId(), getItemType(), getItemId());
    TagsDTO dto = tagBuilder.buildAsMap(tagUri, resourceRequest, user, getDatasetPath(), name);
    return Response.status(Response.Status.OK).entity(dto).build();
  }
  
  @ApiOperation( value = "Delete all attached tags")
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTags(@Context SecurityContext sc,
                             @Context HttpServletRequest req)
    throws DatasetException, MetadataException {
    
    Users user = jwtHelper.getUserPrincipal(sc);
    tagController.deleteAll(user, getDatasetPath());
    return Response.noContent().build();
  }
  
  @ApiOperation( value = "Delete tag attached")
  @DELETE
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTag(@Context SecurityContext sc,
                            @Context HttpServletRequest req,
                            @ApiParam(value = "Name of the tag", required = true) @PathParam("name") String name)
    throws DatasetException, MetadataException {
    
    Users user = jwtHelper.getUserPrincipal(sc);
    tagController.delete(user, getDatasetPath(), name);
    return Response.noContent().build();
  }
}
