/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.tags;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagController;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagType;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

@Logged
@Path("/tags")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN"})
@Api(value = "Feature store tag")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TagsResource {
  private static final Logger LOGGER = Logger.getLogger(TagsResource.class.getName());
  
  @EJB
  private FeatureStoreTagsBuilder tagsBuilder;
  @EJB
  private FeatureStoreTagController featureStoreTagController;
  
  @ApiOperation(value = "Get all tags", response = TagsDTO.class)
  @GET
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(@Context SecurityContext sc, @Context UriInfo uriInfo,
    @BeanParam Pagination pagination, @BeanParam TagsBeanParam tagsBeanParam) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(tagsBeanParam.getSortBySet());
    resourceRequest.setFilter(tagsBeanParam.getFilter());
    TagsDTO dto = tagsBuilder.buildByName(uriInfo, resourceRequest);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get by name", response = TagsDTO.class)
  @GET
  @Path("{name}")
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@Context SecurityContext sc, @Context UriInfo uriInfo, @PathParam("name") String name)
    throws FeatureStoreTagException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, name);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Update tag by name", response = TagsDTO.class)
  @PUT
  @Path("{name}")
  public Response put(@Context SecurityContext sc, @Context UriInfo uriInfo, @PathParam("name") String name,
    @QueryParam("name") String newName, @QueryParam("type") TagType type)
    throws FeatureStoreTagException {
    featureStoreTagController.update(name, newName, type);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, newName);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Create new tag", response = TagsDTO.class)
  @POST
  public Response post(@Context SecurityContext sc, @Context UriInfo uriInfo, @QueryParam("name") String name,
    @QueryParam("type") TagType type) throws FeatureStoreTagException {
    featureStoreTagController.create(name, type);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAGS);
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, name);
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  @ApiOperation(value = "Delete a tag by name")
  @DELETE
  @Path("{name}")
  public Response delete(@Context SecurityContext sc, @Context UriInfo uriInfo, @PathParam("name") String name) {
    featureStoreTagController.delete(name);
    return Response.noContent().build();
  }
  
}