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
package io.hops.hopsworks.api.dataset.tags;

import io.hops.hopsworks.api.tags.TagBuilder;
import io.hops.hopsworks.common.tags.TagControllerIface;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.api.tags.TagsExpansionBeanParam;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Api(value = "Dataset Tags Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatasetTagsResource {
  private static final Logger LOGGER = Logger.getLogger(DatasetTagsResource.class.getName());
  
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private DatasetHelper datasetHelper;
  @Inject
  private TagControllerIface tagsController;
  @EJB
  private TagBuilder tagsBuilder;
  
  private Project project;
  
  public void setProject(Project project) {
    this.project = project;
  }
  
  public void setParams(Project project) {
    this.project = project;
  }
  
  @ApiOperation(value = "Create or update tag for a dataset", response = TagsDTO.class)
  @PUT
  @Path("/schema/{schemaName}/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response putTag(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @ApiParam(value = "Name of the tag", required = true)
                         @PathParam("schemaName") String schemaName,
                         @PathParam("path") String path,
                         @QueryParam("datasetType") DatasetType datasetType,
                         @ApiParam(value = "Value to set for the tag") String value)
    throws DatasetException, MetadataException, SchematizedTagException, GenericException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, datasetType);
    Users user = jWTHelper.getUserPrincipal(sc);
    
    AttachTagResult result = tagsController.upsert(user, datasetPath, schemaName, value);
    TagsDTO dto = tagsBuilder.build(new InodeTagUri(uriInfo), datasetPath, result.getItems());
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @ApiOperation( value = "Create or update tags(bulk) for a featuregroup", response = TagsDTO.class)
  @PUT
  @Path("/bulk/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response bulkPutTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                              @PathParam("path") String path,
                              @QueryParam("datasetType") DatasetType datasetType,
                              TagsDTO tags)
    throws DatasetException, MetadataException, SchematizedTagException, GenericException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, datasetType);
    Users user = jWTHelper.getUserPrincipal(sc);
    
    AttachTagResult result;
    
    if(tags.getItems().size() == 0) {
      result = tagsController.upsert(user, datasetPath, tags.getName(), tags.getValue());
    } else {
      Map<String, String> newTags = new HashMap<>();
      for(TagsDTO tag : tags.getItems()) {
        newTags.put(tag.getName(), tag.getValue());
      }
      result = tagsController.upsertAll(user, datasetPath, newTags);
    }
    
    TagsDTO dto = tagsBuilder.build(new InodeTagUri(uriInfo), datasetPath, result.getItems());
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @ApiOperation( value = "Get all tags attached to a dataset", response = TagsDTO.class)
  @GET
  @Path("/all/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTags(@Context SecurityContext sc, @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @PathParam("path") String path,
                          @QueryParam("datasetType") DatasetType datasetType,
                          @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws DatasetException, SchematizedTagException, MetadataException, GenericException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, datasetType);
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = tagsBuilder.build(new InodeTagUri(uriInfo), resourceRequest, user, datasetPath);
    return Response.status(Response.Status.OK).entity(dto).build();
  }
  
  @ApiOperation( value = "Get tag attached to a dataset", response = TagsDTO.class)
  @GET
  @Path("/schema/{schemaName}/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTag(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @ApiParam(value = "Name of the tag", required = true)
                         @PathParam("schemaName") String schemaName,
                         @PathParam("path") String path,
                         @QueryParam("datasetType") DatasetType datasetType,
                         @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws DatasetException, SchematizedTagException, MetadataException, GenericException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, datasetType);
    Users user = jWTHelper.getUserPrincipal(sc);
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = tagsBuilder.build(new InodeTagUri(uriInfo), resourceRequest, user, datasetPath, schemaName);
    return Response.status(Response.Status.OK).entity(dto).build();
  }
  
  @ApiOperation( value = "Delete all attached tags to a dataset")
  @DELETE
  @Path("/all/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteTags(@Context SecurityContext sc,
                             @Context HttpServletRequest req,
                             @PathParam("path") String path,
                             @QueryParam("datasetType") DatasetType datasetType)
    throws DatasetException, MetadataException, GenericException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, datasetType);
    Users user = jWTHelper.getUserPrincipal(sc);
    
    tagsController.deleteAll(user, datasetPath);
    
    return Response.noContent().build();
  }
  
  @ApiOperation( value = "Delete tag attached to a dataset")
  @DELETE
  @Path("/schema/{schemaName}/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteTag(@Context SecurityContext sc,
                            @Context HttpServletRequest req,
                            @ApiParam(value = "Name of the tag", required = true)
                            @PathParam("schemaName") String schemaName,
                            @PathParam("path") String path,
                            @QueryParam("datasetType") DatasetType datasetType)
    throws DatasetException, MetadataException, GenericException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, datasetType);
    Users user = jWTHelper.getUserPrincipal(sc);
    
    tagsController.delete(user, datasetPath, schemaName);
    
    return Response.noContent().build();
  }
}
