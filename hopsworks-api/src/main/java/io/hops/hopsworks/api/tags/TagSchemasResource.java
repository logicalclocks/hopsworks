/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagSchemaControllerIface;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

@Path("/tags")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN"})
@Api(value = "Feature store tag")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TagSchemasResource {
  private static final Logger LOGGER = Logger.getLogger(TagSchemasResource.class.getName());
  
  @EJB
  private TagSchemasBuilder tagSchemasBuilder;
  @Inject
  private FeatureStoreTagSchemaControllerIface featureStoreTagSchemaController;
  
  @ApiOperation(value = "Get all schemas", response = SchemaDTO.class)
  @GET
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getAll(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @BeanParam Pagination pagination,
                         @BeanParam TagsBeanParam tagsBeanParam) {
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAG_SCHEMAS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(tagsBeanParam.getSortBySet());
    resourceRequest.setFilter(tagsBeanParam.getFilter());
    SchemaDTO dto = tagSchemasBuilder.build(uriInfo, resourceRequest);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get schema by name", response = SchemaDTO.class)
  @GET
  @Path("{name}")
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@Context SecurityContext sc, @Context UriInfo uriInfo,
                      @PathParam("name") String schemaName)
    throws FeatureStoreTagException {
    
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAG_SCHEMAS);
    SchemaDTO dto = tagSchemasBuilder.build(uriInfo, resourceRequest, schemaName);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Create new schema", response = SchemaDTO.class)
  @POST
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles={"HOPS_ADMIN"})
  public Response post(@Context SecurityContext sc, @Context UriInfo uriInfo,
                       @QueryParam("name") String schemaName,
                       String schema)
    throws FeatureStoreTagException {
    
    featureStoreTagSchemaController.create(schemaName, schema);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TAG_SCHEMAS);
    SchemaDTO dto = tagSchemasBuilder.build(uriInfo, resourceRequest, schemaName);
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  @ApiOperation(value = "Delete a schema by name")
  @DELETE
  @Path("{name}")
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles={"HOPS_ADMIN"})
  public Response delete(@Context SecurityContext sc, @Context UriInfo uriInfo,
                         @PathParam("name") String schemaName) {
    
    featureStoreTagSchemaController.delete(schemaName);
    return Response.noContent().build();
  }
}
