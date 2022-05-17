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
package io.hops.hopsworks.api.tags;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.tags.AttachTagResult;
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
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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

@Logged
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Tags resource")
public abstract class TagsResource {
  @EJB
  private JWTHelper jwtHelper;
  
  protected Project project;
  protected Featurestore featurestore;
  
  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureStore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }
  
  protected abstract String get(Users user, String name)
    throws DatasetException, MetadataException, SchematizedTagException;
  
  protected abstract Map<String, String> getAll(Users user)
    throws DatasetException, MetadataException, SchematizedTagException;
  
  protected abstract AttachTagResult upsert(Users user, String name, String value)
    throws MetadataException, SchematizedTagException;
  
  protected abstract AttachTagResult upsert(Users user, Map<String, String> tags)
    throws MetadataException, SchematizedTagException;
  
  protected abstract void delete(Users user, String name)
    throws DatasetException, MetadataException, SchematizedTagException;
  
  protected abstract void deleteAll(Users user)
    throws DatasetException, MetadataException, SchematizedTagException;
  
  protected abstract TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Map<String, String> tags)
    throws DatasetException, SchematizedTagException, MetadataException;
  
  
  @PUT
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putTag(
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "Name of the tag")
    @PathParam("name") String name,
    @ApiParam(value = "Value of the tag") String value)
    throws SchematizedTagException, MetadataException, DatasetException {
    
    Users user = jwtHelper.getUserPrincipal(sc);
    AttachTagResult result = upsert(user, name, value);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = build(uriInfo, resourceRequest, result.getItems());
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response putTags(
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "TagsDTO containing multiple tags") TagsDTO tags)
    throws SchematizedTagException, MetadataException, DatasetException {
    Users user = jwtHelper.getUserPrincipal(sc);
    AttachTagResult result;
    
    if (tags.getItems().size() == 0) {
      result = upsert(user, tags.getName(), tags.getValue());
    } else {
      Map<String, String> newTags = new HashMap<>();
      for (TagsDTO tag : tags.getItems()) {
        newTags.put(tag.getName(), tag.getValue());
      }
      result = upsert(user, newTags);
    }
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = build(uriInfo, resourceRequest, result.getItems());
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if (result.isCreated()) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTag(
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "Name of the tag")
    @PathParam("name") String name,
    @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws SchematizedTagException, MetadataException, DatasetException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Map<String, String> result = new HashMap<>();
    result.put(name, get(user, name));
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = build(uriInfo, resourceRequest, result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTags(
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @BeanParam TagsExpansionBeanParam tagsExpansionBeanParam)
    throws SchematizedTagException, MetadataException, DatasetException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Map<String, String> result = getAll(user);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setExpansions(tagsExpansionBeanParam.getResources());
    TagsDTO dto = build(uriInfo, resourceRequest, result);
    return Response.status(Response.Status.OK).entity(dto).build();
  }
  
  @DELETE
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTag(
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo,
    @ApiParam(value = "Name of the tag")
    @PathParam("name") String name) throws SchematizedTagException, MetadataException, DatasetException {
    Users user = jwtHelper.getUserPrincipal(sc);
    delete(user, name);
    return Response.noContent().build();
  }
  
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteTags(
    @Context SecurityContext sc,
    @Context HttpServletRequest req,
    @Context UriInfo uriInfo) throws SchematizedTagException, MetadataException, DatasetException {
    Users user = jwtHelper.getUserPrincipal(sc);
    deleteAll(user);
    return Response.noContent().build();
  }
}
