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
package io.hops.hopsworks.api.jwt;

import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.Stateless;
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
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Logger;

@Logged
@Path("/jwt")
@Stateless
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JWTRequired(acceptedTokens = {Audience.SERVICES}, allowedUserRoles = {"AGENT"})
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "JWT service", description = "Manage application and service JWT")
public class JWTResource {

  private final static Logger LOGGER = Logger.getLogger(JWTResource.class.getName());

  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private Settings settings;

  @POST
  @ApiOperation(value = "Create application token", response = JWTResponseDTO.class)
  @ApiKeyRequired(acceptedScopes = {ApiScope.AUTH}, allowedUserRoles = {"HOPS_ADMIN", "AGENT"})
  public Response createToken(JWTRequestDTO jWTRequestDTO,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc) throws NoSuchAlgorithmException,
    SigningKeyNotFoundException, DuplicateSigningKeyException {
    JWTResponseDTO jWTResponseDTO = jWTHelper.createToken(jWTRequestDTO, settings.getJWTIssuer());
    return Response.ok().entity(jWTResponseDTO).build();
  }

  @PUT
  @ApiOperation(value = "Renew application token", response = JWTResponseDTO.class)
  @ApiKeyRequired(acceptedScopes = {ApiScope.AUTH}, allowedUserRoles = {"HOPS_ADMIN", "AGENT"})
  public Response renewToken(JsonWebTokenDTO jsonWebTokenDTO,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc)
    throws SigningKeyNotFoundException, NotRenewableException, InvalidationException {
    JWTResponseDTO jWTResponseDTO = jWTHelper.renewToken(jsonWebTokenDTO, true, new HashMap<>(3));
    return Response.ok().entity(jWTResponseDTO).build();
  }
  
  @DELETE
  @Path("/{token}")
  @ApiOperation(value = "Invalidate application token")
  @ApiKeyRequired(acceptedScopes = {ApiScope.AUTH}, allowedUserRoles = {"HOPS_ADMIN", "AGENT"})
  public Response invalidateToken(@ApiParam(value = "Token to invalidate", required = true)
                                  @PathParam("token") String token,
                                  @Context HttpServletRequest req,
                                  @Context SecurityContext sc) throws InvalidationException {
    jWTHelper.invalidateToken(token);
    return Response.ok().build();
  }
  
  @DELETE
  @Path("/key/{keyName}")
  @ApiOperation(value = "Delete a JWT signing key")
  @ApiKeyRequired(acceptedScopes = {ApiScope.AUTH}, allowedUserRoles = {"HOPS_ADMIN", "AGENT"})
  public Response removeSigingKey(
      @ApiParam(value = "Name of the signing key to remove", required = true)
      @PathParam("keyName") String keyName,
      @Context HttpServletRequest req,
      @Context SecurityContext sc) {
    jWTHelper.deleteSigningKeyByName(keyName);
    return Response.ok().build();
  }
  
  @GET
  @Path("/elk/key")
  @ApiOperation(value = "Get the signing key for ELK if exists otherwise " +
      "create a new one and return")
  public Response getSigningKeyforELK(@Context SecurityContext sc,
                                      @Context HttpServletRequest req) throws OpenSearchException {
    String signingKey = jWTHelper.getSigningKeyForELK();
    return Response.ok().entity(signingKey).build();
  }
  
  @GET
  @Path("/elk/token/{projectId}")
  @ApiOperation(value = "Create opensearch jwt token for the provided project as" +
      " Data Owner.")
  public Response createELKTokenAsDataOwner(@PathParam(
      "projectId") Integer projectId, @Context SecurityContext sc) throws OpenSearchException {
    if (projectId == null) {
      throw new IllegalArgumentException("projectId was not provided.");
    }
    OpenSearchJWTResponseDTO
        jWTResponseDTO = jWTHelper.createTokenForELKAsDataOwner(projectId);
    return Response.ok().entity(jWTResponseDTO).build();
  }
}
