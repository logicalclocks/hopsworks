/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.user.apiKey;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.user.security.apiKey.ApiKeyController;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "ApiKey Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ApiKeyResource {
  
  private static final Logger LOGGER = Logger.getLogger(ApiKeyResource.class.getName());
  
  @EJB
  private ApiKeyController apikeyController;
  @EJB
  private ApiKeyBuilder apikeyBuilder;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all api keys.", response = ApiKeyDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@BeanParam Pagination pagination, @BeanParam ApiKeyBeanParam apikeyBeanParam,
    @Context UriInfo uriInfo, @Context SecurityContext sc) {
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.APIKEY);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(apikeyBeanParam.getSortBySet());
    resourceRequest.setFilter(apikeyBeanParam.getFilter());
    ApiKeyDTO dto = apikeyBuilder.buildItems(uriInfo, resourceRequest, user);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Find api key by name.", response = ApiKeyDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByName(@PathParam("name") String name, @Context UriInfo uriInfo, @Context SecurityContext sc)
    throws ApiKeyException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.APIKEY);
    ApiKeyDTO dto = apikeyBuilder.build(uriInfo, resourceRequest, user, name);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("key")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Find api key by name.", response = ApiKeyDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByKey(@QueryParam("key") String key, @Context UriInfo uriInfo, @Context SecurityContext sc)
    throws ApiKeyException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.APIKEY);
    ApiKey apikey = apikeyController.getApiKey(key);
    ApiKeyDTO dto = apikeyBuilder.build(uriInfo, resourceRequest, apikey);
    return Response.ok().entity(dto).build();
  }
  
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update an api key.", response = ApiKeyDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response update(@QueryParam("name") String name, @QueryParam("action") ApiKeyUpdateAction action,
    @QueryParam("scope") Set<ApiScope> scopes, @Context UriInfo uriInfo, @Context HttpServletRequest req,
    @Context SecurityContext sc) throws ApiKeyException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Set<ApiScope> validatedScopes = validateScopes(user, scopes);
    ApiKey apikey;
    switch (action == null ? ApiKeyUpdateAction.ADD : action) {
      case ADD:
        apikey = apikeyController.addScope(user, name, validatedScopes);
        break;
      case DELETE:
        apikey = apikeyController.removeScope(user, name, validatedScopes);
        break;
      case UPDATE:
        apikey = apikeyController.update(user, name, validatedScopes);
        break;
      default:
        throw new WebApplicationException("Action need to set a valid action, but found: " + action,
          Response.Status.NOT_FOUND);
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.APIKEY);
    ApiKeyDTO dto = apikeyBuilder.build(uriInfo, resourceRequest, apikey);
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create an api key.", response = ApiKeyDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "AGENT"})
  public Response create(@QueryParam("name") String name, @QueryParam("scope") Set<ApiScope> scopes,
    @Context UriInfo uriInfo, @Context SecurityContext sc,
    @Context HttpServletRequest req) throws ApiKeyException, UserException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Set<ApiScope> validatedScopes = validateScopes(user, scopes);
    String apiKey = apikeyController.createNewKey(user, name, validatedScopes, false);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.APIKEY);
    ApiKeyDTO dto = apikeyBuilder.build(uriInfo, resourceRequest, user, name);
    dto.setKey(apiKey);
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  @DELETE
  @Path("{name}")
  @ApiOperation(value = "Delete api key by name.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteByName(@PathParam("name") String name, @Context UriInfo uriInfo,
    @Context HttpServletRequest req, @Context SecurityContext sc) throws ApiKeyException {
    Users user = jwtHelper.getUserPrincipal(sc);
    apikeyController.deleteKey(user, name);
    return Response.noContent().build();
  }
  
  @DELETE
  @ApiOperation(value = "Delete all api keys.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteAll(@Context UriInfo uriInfo, @Context SecurityContext sc,
    @Context HttpServletRequest req) throws ApiKeyException {
    Users user = jwtHelper.getUserPrincipal(sc);
    apikeyController.deleteAll(user);
    return Response.noContent().build();
  }
  
  @GET
  @Path("scopes")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all api key scopes.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getScopes(@Context SecurityContext sc) throws UserException {
    Users user = jwtHelper.getUserPrincipal(sc);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    Set<ApiScope> scopes = getScopesForUser(user);
    GenericEntity<Set<ApiScope>> scopeEntity = new GenericEntity<Set<ApiScope>>(scopes) {
    };
    return Response.ok().entity(scopeEntity).build();
  }
  
  @GET
  @Path("session")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Check api key session.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE, ApiScope.DATASET_DELETE, ApiScope.DATASET_VIEW,
    ApiScope.JOB, ApiScope.SERVING}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response checkSession(@Context SecurityContext sc) {
    return Response.ok().build();
  }

  // For a strange reason the Set of user supplied ApiScope(s) is marshalled
  // to String even though it's a Set of ApiScope. We need to explicitly convert
  // them to ApiScope
  private Set<ApiScope> validateScopes(Users user, Set<ApiScope> scopes) throws ApiKeyException {
    Set<ApiScope> validScopes = getScopesForUser(user);
    Set<ApiScope> validatedScopes = new HashSet<>(scopes.size());
    for (Object scope : scopes) {
      try {
        ApiScope apiScope = ApiScope.fromString((String) scope);
        if (!validScopes.contains(apiScope)) {
          throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_SCOPE_CONTROL_EXCEPTION, Level.FINE,
                  "User is not allowed to issue token " + apiScope.name(),
                  "User " + user.getUsername() + " tried to generate API key with scope " + apiScope
                          + " but it's role is not allowed to");
        }
        validatedScopes.add(apiScope);
      } catch (IllegalArgumentException iae) {
        throw new WebApplicationException("Scope need to set a valid scope, but found: " + scope,
                Response.Status.NOT_FOUND);
      }
    }
    return validatedScopes;
  }

  private Set<ApiScope> getScopesForUser(Users user) {
    BbcGroup adminGroup = bbcGroupFacade.findByGroupName("HOPS_ADMIN");
    if (user.getBbcGroupCollection().contains(adminGroup)) {
      return ApiScope.getAll();
    }
    return ApiScope.getUnprivileged();
  }
}